package it.bitrock.kafkaflightstream.producer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.kafkaflightstream.producer.config.AppConfig
import it.bitrock.kafkaflightstream.producer.model._
import it.bitrock.kafkaflightstream.producer.routes.Routes
import it.bitrock.kafkaflightstream.producer.services.MainFunctions._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Main extends App with LazyLogging {

  implicit val system: ActorSystem    = ActorSystem("KafkaFlightstreamProducer")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext   = system.dispatcher

  logger.info("Starting up")

  val config = AppConfig.load
  logger.debug(s"Loaded configuration: $config")

  val host: String  = config.server.host
  val port: Int     = config.server.port
  val routes        = new Routes(config.server)
  val bindingFuture = Http().bindAndHandle(routes.routes, host, port)

  bindingFuture.map { serverBinding =>
    logger.info(s"Exposing to ${serverBinding.localAddress}")
  }

  val cancellableFlight = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.flightStream.pollingStart,
    config.aviation.flightStream.pollingInterval,
    config.aviation.getAviationUri(FlightStream),
    config.aviation.apiTimeout,
    config.kafka.flightRawTopic,
    FlightStream,
    filterFlight
  )

  val cancellableAirplane = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.airplaneStream.pollingStart,
    config.aviation.airplaneStream.pollingInterval,
    config.aviation.getAviationUri(AirplaneStream),
    config.aviation.apiTimeout,
    config.kafka.airplaneRawTopic,
    AirplaneStream
  )

  val cancellableAirport = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.airportStream.pollingStart,
    config.aviation.airportStream.pollingInterval,
    config.aviation.getAviationUri(AirportStream),
    config.aviation.apiTimeout,
    config.kafka.airportRawTopic,
    AirportStream
  )

  val cancellableAirline = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.airlineStream.pollingStart,
    config.aviation.airlineStream.pollingInterval,
    config.aviation.getAviationUri(AirlineStream),
    config.aviation.apiTimeout,
    config.kafka.airlineRawTopic,
    AirlineStream,
    filterAirline
  )

  val cancellableCity = runStream(
    config.kafka.schemaRegistryUrl,
    config.aviation.cityStream.pollingStart,
    config.aviation.cityStream.pollingInterval,
    config.aviation.getAviationUri(CityStream),
    config.aviation.apiTimeout,
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
    val resourcesClosed = for {
      binding <- bindingFuture
      _       <- binding.terminate(hardDeadline = 3.seconds)
      t       <- system.terminate()
    } yield t
    Await.result(resourcesClosed, 10.seconds)
  }

}
