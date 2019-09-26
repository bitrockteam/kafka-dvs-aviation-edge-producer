package it.bitrock.kafkaflightstream.producer.services

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import it.bitrock.kafkaflightstream.producer.config.{AppConfig, AviationConfig, KafkaConfig, ServerConfig}
import it.bitrock.kafkaflightstream.producer.kafka.KafkaSinkFactory
import it.bitrock.kafkaflightstream.producer.kafka.KafkaTypes.{Airline, Airplane, Airport, City, Flight, Key}
import it.bitrock.kafkaflightstream.producer.model.{
  AirlineMessageJson,
  AirlineStream,
  AirplaneStream,
  AirportStream,
  AviationStream,
  CityStream,
  FlightMessageJson,
  FlightStream,
  MessageJson
}
import it.bitrock.kafkaflightstream.producer.routes.Routes
import it.bitrock.kafkageostream.kafkacommons.serialization.AvroSerdes
import org.apache.kafka.common.serialization.Serdes

import scala.concurrent.{ExecutionContext, Future}

object MainFunctions {

  val serverConfig: ServerConfig     = AppConfig.server
  val aviationConfig: AviationConfig = AppConfig.aviation
  val kafkaConfig: KafkaConfig       = AppConfig.kafka

  def openHealthCheckPort()(implicit system: ActorSystem, mat: ActorMaterializer): Future[Http.ServerBinding] = {
    val host   = serverConfig.host
    val port   = serverConfig.port
    val routes = new Routes(serverConfig)
    Http().bindAndHandle(routes.routes, host, port)
  }

  def runStream(streamType: AviationStream)(implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Cancellable = {

    val source = new TickSource(
      aviationConfig.getAviationStreamConfig(streamType).pollingStart,
      aviationConfig.getAviationStreamConfig(streamType).pollingInterval
    ).source

    val flow = new AviationFlow().flow(aviationConfig.getAviationUri(streamType), aviationConfig.apiTimeout)

    val keySerializer = Serdes.String().serializer
    val sink = streamType match {
      case FlightStream =>
        val flightRawSerializer    = AvroSerdes.serdeFrom[Flight.Value](kafkaConfig.schemaRegistryUrl).serializer
        val flightProducerSettings = ProducerSettings(system, keySerializer, flightRawSerializer)
        new KafkaSinkFactory[MessageJson, Key, Flight.Value](kafkaConfig.flightRawTopic, flightProducerSettings).sink
      case AirplaneStream =>
        val airplaneRawSerializer    = AvroSerdes.serdeFrom[Airplane.Value](kafkaConfig.schemaRegistryUrl).serializer
        val airplaneProducerSettings = ProducerSettings(system, keySerializer, airplaneRawSerializer)
        new KafkaSinkFactory[MessageJson, Key, Airplane.Value](kafkaConfig.airplaneRawTopic, airplaneProducerSettings).sink
      case AirportStream =>
        val airportRawSerializer    = AvroSerdes.serdeFrom[Airport.Value](kafkaConfig.schemaRegistryUrl).serializer
        val airportProducerSettings = ProducerSettings(system, keySerializer, airportRawSerializer)
        new KafkaSinkFactory[MessageJson, Key, Airport.Value](kafkaConfig.airportRawTopic, airportProducerSettings).sink
      case AirlineStream =>
        val airlineRawSerializer    = AvroSerdes.serdeFrom[Airline.Value](kafkaConfig.schemaRegistryUrl).serializer
        val airlineProducerSettings = ProducerSettings(system, keySerializer, airlineRawSerializer)
        new KafkaSinkFactory[MessageJson, Key, Airline.Value](kafkaConfig.airlineRawTopic, airlineProducerSettings).sink
      case CityStream =>
        val cityRawSerializer    = AvroSerdes.serdeFrom[City.Value](kafkaConfig.schemaRegistryUrl).serializer
        val cityProducerSettings = ProducerSettings(system, keySerializer, cityRawSerializer)
        new KafkaSinkFactory[MessageJson, Key, City.Value](kafkaConfig.cityRawTopic, cityProducerSettings).sink
    }

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

  private def validFlightStatus(status: String): Boolean                              = status == "en-route"
  private def validFlightSpeed(speed: Double): Boolean                                = speed < aviationConfig.flightSpeedLimit
  private def validFlightJourney(departureCode: String, arrivalCode: String): Boolean = !(departureCode.isEmpty || arrivalCode.isEmpty)

}
