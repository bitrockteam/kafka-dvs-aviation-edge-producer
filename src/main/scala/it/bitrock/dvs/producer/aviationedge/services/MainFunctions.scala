package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import it.bitrock.dvs.producer.aviationedge.config.{AppConfig, AviationConfig, KafkaConfig, ServerConfig}
import it.bitrock.dvs.producer.aviationedge.model._
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
  ): (Cancellable, Future[Done], Future[Done]) = {

    val config = AviationStreamContext[A].config(aviationConfig)

    val tickSource     = new TickSource(config.pollingStart, config.pollingInterval, aviationConfig.tickSource).source
    val aviationFlow   = new AviationFlow().flow(aviationConfig.getAviationUri(config.path), aviationConfig.apiTimeout)
    val rawSink        = AviationStreamContext[A].sink(kafkaConfig)
    val errorSink      = SideStreamContext.errorSink(kafkaConfig)
    val monitoringSink = SideStreamContext.monitoringSink(kafkaConfig)

    val jsonSource = tickSource.via(aviationFlow).via(monitoringGraph(monitoringSink)).mapConcat(identity)

    mainGraph(jsonSource, rawSink, errorSink).run()

  }

  def filterFunction: MessageJson => Boolean = {
    case msg: AirlineMessageJson => filterAirline(msg)
    case msg: FlightMessageJson  => filterFlight(msg)
    case _                       => true
  }

  def filterAirline(airline: AirlineMessageJson): Boolean = airline.statusAirline == "active"

  def filterFlight(flight: FlightMessageJson): Boolean =
    validFlightStatus(flight.status) &&
      validFlightSpeed(flight.speed.horizontal) &&
      validFlightJourney(flight.departure.iataCode, flight.arrival.iataCode)

  private def validFlightStatus(status: String): Boolean = status == "en-route"
  private def validFlightSpeed(speed: Double): Boolean   = speed < aviationConfig.flightSpeedLimit
  private def validFlightJourney(departureCode: String, arrivalCode: String): Boolean =
    departureCode.nonEmpty && arrivalCode.nonEmpty

}
