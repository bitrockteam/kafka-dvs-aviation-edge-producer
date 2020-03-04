package it.bitrock.dvs.producer.aviationedge.services

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Partition, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, FlowShape}
import it.bitrock.dvs.producer.aviationedge.model.PartitionPorts._
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.dvs.producer.aviationedge.services.MainFunctions.aviationConfig

object Graphs {
  def mainGraph[Message, SourceMat, SinkMat](
      jsonSource: Source[Either[ErrorMessageJson, Message], SourceMat],
      rawSink: Sink[Message, SinkMat],
      errorSink: Sink[ErrorMessageJson, SinkMat],
      invalidFlightSink: Sink[FlightMessageJson, SinkMat]
  ): RunnableGraph[(SourceMat, SinkMat, SinkMat, SinkMat)] = RunnableGraph.fromGraph(
    GraphDSL.create(jsonSource, rawSink, errorSink, invalidFlightSink)((a, b, c, d) => (a, b, c, d)) {
      implicit builder => (source, raw, error, invalidFlight) =>
        import GraphDSL.Implicits._

        val partition     = builder.add(Partition[Either[ErrorMessageJson, Message]](3, partitionMessages))
        val collectErrors = builder.add(Flow[Either[ErrorMessageJson, Message]].collect { case Left(x) => x })
        val collectRaws =
          builder.add(Flow[Either[ErrorMessageJson, Message]].collect { case Right(x) => x }.filter(filterFunction))
        val collectInvalidFlight = builder.add(
          Flow
            .fromFunction[Either[ErrorMessageJson, Message], Option[FlightMessageJson]] {
              case Right(x: FlightMessageJson) => Some(x)
              case _                           => None
            }
            .collect { case Some(x) => x }
        )

        source ~> partition
        partition.out(ErrorPort) ~> collectErrors ~> error
        partition.out(RawPort) ~> collectRaws ~> raw
        partition.out(InvalidPort) ~> collectInvalidFlight ~> invalidFlight

        ClosedShape
    }
  )

  def collectRightMessagesGraph[Message, SourceMat, SinkMat](
      jsonSource: Source[Either[ErrorMessageJson, Message], SourceMat],
      rawSink: Sink[Message, SinkMat]
  ): RunnableGraph[(SourceMat, SinkMat)] =
    jsonSource.collect { case Right(v) => v }.toMat(rawSink)(Keep.both)

  def monitoringGraph[Message, SinkMat](
      rawSink: Sink[MonitoringMessageJson, SinkMat]
  ): Flow[List[Either[ErrorMessageJson, Message]], List[Either[ErrorMessageJson, Message]], SinkMat] = Flow.fromGraph(
    GraphDSL.create(rawSink) { implicit builder => sink =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[List[Either[ErrorMessageJson, Message]]](2))
      val flowShape = builder.add(monitoringFlow)

      broadcast.out(0) ~> flowShape ~> sink

      FlowShape(broadcast.in, broadcast.out(1))
    }
  )

  private def monitoringFlow[Message]: Flow[List[Either[ErrorMessageJson, Message]], MonitoringMessageJson, NotUsed] =
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

  def filterFunction[A](message: A): Boolean = message match {
    case airline: AirlineMessageJson => airline.statusAirline == "active"
    case _                           => true
  }

  def partitionMessages[A](message: Either[ErrorMessageJson, A]): Int = message match {
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
