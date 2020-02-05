package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.stream.ClosedShape
import akka.stream.scaladsl.{GraphDSL, Partition, RunnableGraph, Sink, Source}
import it.bitrock.dvs.producer.aviationedge.config.{AppConfig, AviationConfig, KafkaConfig, ServerConfig}
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.dvs.producer.aviationedge.routes.Routes

import scala.concurrent.{ExecutionContext, Future}

object MainFunctions {

  val serverConfig: ServerConfig     = AppConfig.server
  val aviationConfig: AviationConfig = AppConfig.aviation
  val kafkaConfig: KafkaConfig       = AppConfig.kafka

  def bindRoutes()(implicit system: ActorSystem): Future[Http.ServerBinding] = {
    val host   = serverConfig.host
    val port   = serverConfig.port
    val routes = new Routes(serverConfig)
    Http().bindAndHandle(routes.routes, host, port)
  }

  def runStream[A: AviationStreamContext]()(
      implicit system: ActorSystem,
      ec: ExecutionContext
  ): (Cancellable, Future[Done], Future[Done], Future[Done]) = {

    val config = AviationStreamContext[A].config(aviationConfig)

    val tickSource   = new TickSource(config.pollingStart, config.pollingInterval, aviationConfig.tickSource).source
    val aviationFlow = new AviationFlow().flow(aviationConfig.getAviationUri(config.path), aviationConfig.apiTimeout)

    val jsonSource        = tickSource.via(aviationFlow).mapConcat(identity)
    val rawSink           = AviationStreamContext[A].sink(kafkaConfig)
    val errorSink         = ErrorStreamContext.sink(kafkaConfig)
    val invalidFlightSink = AviationStreamContext[A].sink(kafkaConfig)

    buildGraph(jsonSource, rawSink, errorSink, invalidFlightSink).run()
  }

  def buildGraph[SourceMat, SinkMat](
      jsonSource: Source[Either[ErrorMessageJson, MessageJson], SourceMat],
      rawSink: Sink[MessageJson, SinkMat],
      errorSink: Sink[ErrorMessageJson, SinkMat],
      invalidFlightSink: Sink[MessageJson, SinkMat]
  ): RunnableGraph[(SourceMat, SinkMat, SinkMat, SinkMat)] = RunnableGraph.fromGraph(
    GraphDSL.create(jsonSource, rawSink, errorSink, invalidFlightSink)((a, b, c, d) => (a, b, c, d)) { implicit builder => (source, raw, error, invalidFlight) =>
      import GraphDSL.Implicits._

      val partition = builder.add(Partition[Either[ErrorMessageJson, MessageJson]](3, partitionMessages))

      source ~> partition.in

      partition.out(0).map(_.left.get) ~> error
      partition.out(1).map(_.right.get).filter(filterFunction) ~> raw
      partition.out(2).map(_.right.get) ~> invalidFlight

      ClosedShape
    }
  )

  def filterFunction(message: MessageJson): Boolean = message match {
    case airline: AirlineMessageJson => airline.statusAirline == "active"
    case _                           => true
  }

  private def partitionMessages(message: Either[ErrorMessageJson, MessageJson]): Int = message match {
    case Left(_)                        => 0
    case Right(msg: FlightMessageJson)  => if (filterFlight(msg)) 1 else 2
    case Right(_)                       => 1
  }

  private def filterFlight(flight: FlightMessageJson): Boolean =
    validFlightStatus(flight.status) &&
      validFlightSpeed(flight.speed.horizontal) &&
      validFlightJourney(flight.departure.iataCode, flight.arrival.iataCode)

  private def validFlightStatus(status: String): Boolean = status == "en-route"
  private def validFlightSpeed(speed: Double): Boolean   = speed < aviationConfig.flightSpeedLimit
  private def validFlightJourney(departureCode: String, arrivalCode: String): Boolean =
    departureCode.nonEmpty && arrivalCode.nonEmpty

}
