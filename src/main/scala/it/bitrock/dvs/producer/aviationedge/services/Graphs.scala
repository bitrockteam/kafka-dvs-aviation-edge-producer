package it.bitrock.dvs.producer.aviationedge.services

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Partition, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, FlowShape}
import it.bitrock.dvs.producer.aviationedge.model.PartitionPorts._
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.dvs.producer.aviationedge.services.MainFunctions.aviationConfig

object Graphs {
  def mainGraph[SourceMat, SinkMat](
      jsonSource: Source[Either[ErrorMessageJson, MessageJson], SourceMat],
      rawSink: Sink[MessageJson, SinkMat],
      errorSink: Sink[ErrorMessageJson, SinkMat],
      invalidFlightSink: Sink[MessageJson, SinkMat]
  ): RunnableGraph[(SourceMat, SinkMat, SinkMat, SinkMat)] = RunnableGraph.fromGraph(
    GraphDSL.create(jsonSource, rawSink, errorSink, invalidFlightSink)((a, b, c, d) => (a, b, c, d)) {
      implicit builder => (source, raw, error, invalidFlight) =>
        import GraphDSL.Implicits._

        val partition     = builder.add(Partition[Either[ErrorMessageJson, MessageJson]](3, partitionMessages))
        val collectErrors = builder.add(Flow[Either[ErrorMessageJson, MessageJson]].collect { case Left(x) => x })
        val collectRaws =
          builder.add(Flow[Either[ErrorMessageJson, MessageJson]].collect { case Right(x) => x }.filter(filterFunction))
        val collectInvalid = builder.add(Flow[Either[ErrorMessageJson, MessageJson]].collect { case Right(x) => x })

        source ~> partition
        partition.out(ErrorPort) ~> collectErrors ~> error
        partition.out(RawPort) ~> collectRaws ~> raw
        partition.out(InvalidPort) ~> collectInvalid ~> invalidFlight

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
        case Right(msg) if validFlight(msg) => msg
      }

      val numValid       = validFlights.size
      val minUpdated     = validFlights.view.map(_.system.updated).reduceOption(_ min _)
      val averageUpdated = validFlights.view.map(_.system.updated).reduceOption(_ + _).map(_ / numValid)
      val maxUpdated     = validFlights.view.map(_.system.updated).reduceOption(_ max _)
      val numErrors      = flightMessages.count(_.isLeft)
      val total          = flightMessages.size
      val numInvalid     = total - numValid - numErrors

      MonitoringMessageJson(
        messageReceivedOn = Instant.now,
        minUpdated = minUpdated.map(Instant.ofEpochSecond),
        maxUpdated = maxUpdated.map(Instant.ofEpochSecond),
        averageUpdated = averageUpdated.map(Instant.ofEpochSecond),
        numErrors = numErrors,
        numValid = numValid,
        numInvalid = numInvalid,
        total = total
      )
    }

  def filterFunction(message: MessageJson): Boolean = message match {
    case airline: AirlineMessageJson => airline.statusAirline == "active"
    case _                           => true
  }

  def partitionMessages(message: Either[ErrorMessageJson, MessageJson]): Int = message match {
    case Left(_)                                            => ErrorPort
    case Right(msg: FlightMessageJson) if !validFlight(msg) => InvalidPort
    case Right(_)                                           => RawPort
  }

  private def validFlight(flight: FlightMessageJson): Boolean =
    validFlightSpeed(flight.speed.horizontal) &&
      validFlightJourney(flight.departure.iataCode, flight.arrival.iataCode)

  private def validFlightSpeed(speed: Double): Boolean = speed < aviationConfig.flightSpeedLimit
  private def validFlightJourney(departureCode: String, arrivalCode: String): Boolean =
    departureCode.nonEmpty && arrivalCode.nonEmpty
}
