package it.bitrock.dvs.producer.aviationedge.services

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, FlowShape}
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.dvs.producer.aviationedge.services.MainFunctions.aviationConfig

object Graphs {

  def mainGraph[SourceMat, SinkMat](
      jsonSource: Source[Either[ErrorMessageJson, MessageJson], SourceMat],
      rawSink: Sink[MessageJson, SinkMat],
      errorSink: Sink[ErrorMessageJson, SinkMat]
  ): RunnableGraph[(SourceMat, SinkMat, SinkMat)] = RunnableGraph.fromGraph(
    GraphDSL.create(jsonSource, rawSink, errorSink)((x, y, z) => (x, y, z)) { implicit builder => (source, rightSink, leftSink) =>
      import GraphDSL.Implicits._

      val broadcast             = builder.add(Broadcast[Either[ErrorMessageJson, MessageJson]](2))
      val collectRight          = builder.add(Flow[Either[ErrorMessageJson, MessageJson]].collect { case Right(x) => x })
      val collectLeft           = builder.add(Flow[Either[ErrorMessageJson, MessageJson]].collect { case Left(x) => x })
      val filterInvalidMessages = builder.add(Flow[MessageJson].filter(filterFunction))

      source ~> broadcast
      broadcast.out(0) ~> collectRight ~> filterInvalidMessages ~> rightSink
      broadcast.out(1) ~> collectLeft ~> leftSink

      ClosedShape
    }
  )

  def monitoringGraph[SinkMat](
      rawSink: Sink[MonitoringMessageJson, SinkMat]
  ): Flow[List[Either[ErrorMessageJson, MessageJson]], List[Either[ErrorMessageJson, MessageJson]], SinkMat] = Flow.fromGraph(
    GraphDSL.create(rawSink) { implicit builder => sink =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[List[Either[ErrorMessageJson, MessageJson]]](2))
      val flowShape = builder.add(monitoringFlow)

      broadcast.out(0) ~> flowShape ~> sink

      FlowShape(broadcast.in, broadcast.out(1))
    }
  )

  private def monitoringFlow: Flow[List[Either[ErrorMessageJson, MessageJson]], MonitoringMessageJson, NotUsed] =
    Flow.fromFunction { messages =>
      val flightMessages = messages.collect {
        case Right(msg: FlightMessageJson)                                       => Right(msg)
        case Left(e) if e.errorSource.contains(aviationConfig.flightStream.path) => Left(e)
      }
      val validFlights = flightMessages.collect {
        case Right(msg) if filterFlight(msg) => msg
      }

      val minUpdated = validFlights.minBy(_.system.updated).system.updated
      val maxUpdated = validFlights.maxBy(_.system.updated).system.updated
      val numErrors  = flightMessages.count(_.isLeft)
      val numValid   = validFlights.size
      val numInvalid = flightMessages.size - numValid - numErrors

      MonitoringMessageJson(
        Instant.now,
        Instant.ofEpochSecond(minUpdated),
        Instant.ofEpochSecond(maxUpdated),
        numErrors,
        numValid,
        numInvalid
      )
    }

  def filterFunction: MessageJson => Boolean = {
    case msg: AirlineMessageJson => filterAirline(msg)
    case msg: FlightMessageJson  => filterFlight(msg)
    case _                       => true
  }

  private def filterAirline(airline: AirlineMessageJson): Boolean = airline.statusAirline == "active"

  private def filterFlight(flight: FlightMessageJson): Boolean =
    validFlightStatus(flight.status) &&
      validFlightSpeed(flight.speed.horizontal) &&
      validFlightJourney(flight.departure.iataCode, flight.arrival.iataCode)

  private def validFlightStatus(status: String): Boolean = status == "en-route"
  private def validFlightSpeed(speed: Double): Boolean   = speed < aviationConfig.flightSpeedLimit
  private def validFlightJourney(departureCode: String, arrivalCode: String): Boolean =
    departureCode.nonEmpty && arrivalCode.nonEmpty

}
