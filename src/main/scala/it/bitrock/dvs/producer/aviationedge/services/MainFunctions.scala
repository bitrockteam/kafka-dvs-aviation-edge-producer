package it.bitrock.dvs.producer.aviationedge.services

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
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

  def runStream[A: AviationStreamContext]()(implicit system: ActorSystem, ec: ExecutionContext): Cancellable = {

    val config = AviationStreamContext[A].config(aviationConfig)

    val tickSource   = new TickSource(config.pollingStart, config.pollingInterval, aviationConfig.tickSource).source
    val aviationFlow = new AviationFlow().flow(aviationConfig.getAviationUri(config.path), aviationConfig.apiTimeout)

    val jsonSource = tickSource.via(aviationFlow).mapConcat(identity)
    val rawSink    = AviationStreamContext[A].sink(kafkaConfig)
    val errorSink  = ErrorStreamContext.sink(kafkaConfig)

    buildGraph(jsonSource, rawSink, errorSink).run()

  }

  def buildGraph[SourceMat, SinkMat](
      jsonSource: Source[Either[ErrorMessageJson, MessageJson], SourceMat],
      rawSink: Sink[MessageJson, SinkMat],
      errorSink: Sink[ErrorMessageJson, SinkMat]
  ): RunnableGraph[SourceMat] = RunnableGraph.fromGraph(
    GraphDSL.create(jsonSource) { implicit builder => source =>
      import GraphDSL.Implicits._

      val broadcast             = builder.add(Broadcast[Either[ErrorMessageJson, MessageJson]](2))
      val collectRight          = builder.add(Flow[Either[ErrorMessageJson, MessageJson]].collect { case Right(x) => x })
      val collectLeft           = builder.add(Flow[Either[ErrorMessageJson, MessageJson]].collect { case Left(x) => x })
      val filterInvalidMessages = builder.add(Flow[MessageJson].filter(filterFunction))

      source ~> broadcast
      broadcast.out(0) ~> collectRight ~> filterInvalidMessages ~> rawSink
      broadcast.out(1) ~> collectLeft ~> errorSink

      ClosedShape
    }
  )

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
