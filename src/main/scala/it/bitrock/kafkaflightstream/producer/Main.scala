package it.bitrock.kafkaflightstream.producer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.kafkaflightstream.producer.config.AppConfig
import it.bitrock.kafkaflightstream.producer.kafka.KafkaSinkFactory
import it.bitrock.kafkaflightstream.producer.kafka.KafkaTypes.Flight
import it.bitrock.kafkaflightstream.producer.model.FlightMessageJson
import it.bitrock.kafkaflightstream.producer.services.FlightClient
import it.bitrock.kafkageostream.kafkacommons.serialization.AvroSerdes
import org.apache.kafka.common.serialization.Serdes

import scala.concurrent.duration._

object Main extends App with LazyLogging {
  logger.info("Starting up")

  val config = AppConfig.load
  logger.debug(s"Loaded configuration: $config")

  implicit val system: ActorSystem    = ActorSystem("KafkaFlightstreamProducer")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val flightKeySerde = Serdes.String()
  val flightRawSerde = AvroSerdes.serdeFrom[Flight.Value](config.kafka.schemaRegistryUrl)

  val flightProducerSettings = ProducerSettings(
    system,
    flightKeySerde.serializer,
    flightRawSerde.serializer
  )




  val flightClient = new FlightClient()

  val source =
    Source.tick(0.seconds, 30.seconds, HttpRequest(HttpMethods.GET, config.aviation.flightStream.getAviationUri()))

  val flightSinkFactory = new KafkaSinkFactory[FlightMessageJson, Flight.Key, Flight.Value](
    config.kafka.flightRawTopic,
    flightProducerSettings
  )

  val cancellableFlightSource = source
    .via(flightClient.flightRequest)
    .mapAsync(4)(identity)
    .mapConcat(identity)
    .to(flightSinkFactory.sink)
    .run()

  sys.addShutdownHook {
    logger.info("Shutting down")
    cancellableFlightSource.cancel()
  }

}
