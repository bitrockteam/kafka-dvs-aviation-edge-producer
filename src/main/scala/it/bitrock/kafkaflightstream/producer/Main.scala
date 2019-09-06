package it.bitrock.kafkaflightstream.producer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.kafkaflightstream.producer.config.AppConfig
import it.bitrock.kafkaflightstream.producer.model._
import it.bitrock.kafkaflightstream.producer.services.MainFunctions._

object Main extends App with LazyLogging {
  logger.info("Starting up")

  val config = AppConfig.load
  logger.debug(s"Loaded configuration: $config")

  implicit val system: ActorSystem    = ActorSystem("KafkaFlightstreamProducer")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val cancellableFlight = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.flightStream.pollingInterval,
    config.aviation.getAviationUri(FlightStream),
    config.kafka.flightRawTopic,
    FlightStream,
    filterFlight
  )

  val cancellableAirplane = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.airplaneStream.pollingInterval,
    config.aviation.getAviationUri(AirplaneStream),
    config.kafka.airplaneRawTopic,
    AirplaneStream
  )

  val cancellableAirport = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.airportStream.pollingInterval,
    config.aviation.getAviationUri(AirportStream),
    config.kafka.airportRawTopic,
    AirportStream
  )

  val cancellableAirline = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.airlineStream.pollingInterval,
    config.aviation.getAviationUri(AirlineStream),
    config.kafka.airlineRawTopic,
    AirlineStream,
    filterAirline
  )

  val cancellableCity = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.cityStream.pollingInterval,
    config.aviation.getAviationUri(CityStream),
    config.kafka.cityRawTopic,
    CityStream
  )

  sys.addShutdownHook {
    logger.info("Shutting down")
    cancellableFlight.cancel()
    cancellableAirplane.cancel()
    cancellableAirport.cancel()
    cancellableAirline.cancel()
    cancellableCity.cancel()
  }

}
