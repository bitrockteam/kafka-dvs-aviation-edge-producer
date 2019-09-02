package it.bitrock.kafkaflightstream.producer

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.kafkaflightstream.producer.config.AppConfig
import it.bitrock.kafkaflightstream.producer.kafka.KafkaSinkFactory
import it.bitrock.kafkaflightstream.producer.kafka.KafkaTypes.Flight
import it.bitrock.kafkaflightstream.producer.model.FlightMessageJson
import it.bitrock.kafkaflightstream.producer.services.{FlightFlow, TickSource}
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

  val flightFlow = new FlightFlow()

  val flightSource = new TickSource(0.seconds, 30.seconds)

  val flightSinkFactory = new KafkaSinkFactory[FlightMessageJson, Flight.Key, Flight.Value](
    config.kafka.flightRawTopic,
    flightProducerSettings
  )

  val cancellableFlightSource = flightSource.source
    .via(flightFlow.requestFlow(config.aviation.flightStream.getAviationUri()))
    .via(flightFlow.unmarshalFlow)
    .mapConcat(identity)
    .to(flightSinkFactory.sink)
    .run()

  sys.addShutdownHook {
    logger.info("Shutting down")
    cancellableFlightSource.cancel()
  }

}
