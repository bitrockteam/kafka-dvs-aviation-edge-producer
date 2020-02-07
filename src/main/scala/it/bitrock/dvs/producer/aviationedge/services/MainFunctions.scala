package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Partition, RunnableGraph, Sink, Source}
import it.bitrock.dvs.producer.aviationedge.config.{AppConfig, AviationConfig, KafkaConfig, ServerConfig}
import it.bitrock.dvs.producer.aviationedge.routes.Routes
import it.bitrock.dvs.producer.aviationedge.services.Graphs._

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

    val tickSource     = new TickSource(config.pollingStart, config.pollingInterval, aviationConfig.tickSource).source
    val aviationFlow   = new AviationFlow().flow(aviationConfig.getAviationUri(config.path), aviationConfig.apiTimeout)
    val rawSink        = AviationStreamContext[A].sink(kafkaConfig)
    val errorSink      = SideStreamContext.errorSink(kafkaConfig)
    val invalidFlightSink = InvalidFlightStreamContext.sink(kafkaConfig)
    val monitoringSink = SideStreamContext.monitoringSink(kafkaConfig)

    val jsonSource = tickSource.via(aviationFlow).via(monitoringGraph(monitoringSink)).mapConcat(identity)

    mainGraph(jsonSource, rawSink, errorSink, invalidFlightSink).run()

  }


  def buildGraph[SourceMat, SinkMat](
      jsonSource: Source[Either[ErrorMessageJson, MessageJson], SourceMat],
      rawSink: Sink[MessageJson, SinkMat],
      errorSink: Sink[ErrorMessageJson, SinkMat],
      invalidFlightSink: Sink[MessageJson, SinkMat]
  ): RunnableGraph[(SourceMat, SinkMat, SinkMat, SinkMat)] = RunnableGraph.fromGraph(
    GraphDSL.create(jsonSource, rawSink, errorSink, invalidFlightSink)((a, b, c, d) => (a, b, c, d)) { implicit builder => (source, raw, error, invalidFlight) =>
      import GraphDSL.Implicits._

      val broadcast    = builder.add(Broadcast[Either[ErrorMessageJson, MessageJson]](2))
      val collectRight = builder.add(Flow[Either[ErrorMessageJson, MessageJson]].collect { case Right(x) => x })
      val collectLeft  = builder.add(Flow[Either[ErrorMessageJson, MessageJson]].collect { case Left(x) => x })
      val partition    = builder.add(Partition[MessageJson](2, partitionMessages))

      source ~> broadcast

      broadcast.out(0) ~> collectRight ~> partition.in

      partition.out(0).filter(filterFunction) ~> raw
      partition.out(1) ~> invalidFlight

      broadcast.out(1) ~> collectLeft ~> error

      ClosedShape
    }
  )

  def filterFunction(message: MessageJson): Boolean = message match {
    case airline: AirlineMessageJson => airline.statusAirline == "active"
    case _                           => true
  }

  def partitionMessages(message: MessageJson): Int = message match {
    case msg: FlightMessageJson  => if (filterFlight(msg)) 0 else 1
    case _                       => 0
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
