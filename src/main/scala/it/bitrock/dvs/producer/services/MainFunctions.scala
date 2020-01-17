package it.bitrock.dvs.producer.services

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import it.bitrock.dvs.producer.config.{AppConfig, AviationConfig, KafkaConfig, ServerConfig}
import it.bitrock.dvs.producer.model._
import it.bitrock.dvs.producer.routes.Routes

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
    val source = new TickSource(config.pollingStart, config.pollingInterval, aviationConfig.tickSource).source
    val flow   = new AviationFlow().flow(aviationConfig.getAviationUri(config.path), aviationConfig.apiTimeout)
    val sink   = AviationStreamContext[A].sink(kafkaConfig)

    source.via(flow).mapConcat(identity).filter(filterFunction).to(sink).run()
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
  private def validFlightJourney(departureCode: Option[String], arrivalCode: Option[String]): Boolean =
    !(departureCode.getOrElse("").isEmpty || arrivalCode.getOrElse("").isEmpty)

}
