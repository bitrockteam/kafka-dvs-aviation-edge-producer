package it.bitrock.kafkaflightstream.producer

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.kafkaflightstream.producer.config.AppConfig
import it.bitrock.kafkaflightstream.producer.kafka.KafkaSinkFactory
import it.bitrock.kafkaflightstream.producer.kafka.KafkaTypes.Flight
import it.bitrock.kafkaflightstream.producer.model.FlightMessageJson
import it.bitrock.kafkaflightstream.producer.routes.Routes
import it.bitrock.kafkaflightstream.producer.services.FlightClient
import it.bitrock.kafkageostream.kafkacommons.serialization.AvroSerdes
import org.apache.kafka.common.serialization.Serdes

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with LazyLogging {
  logger.info("Starting up")

  val config = AppConfig.load
  logger.debug(s"Loaded configuration: $config")

  val host: String = config.server.host
  val port: Int    = config.server.port

  implicit val system: ActorSystem    = ActorSystem("KafkaGeostreamMeetupProducer")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val flightKeySerde = Serdes.String()
  val flightRawSerde = AvroSerdes.serdeFrom[Flight.Value](config.kafka.schemaRegistryUrl)

  val flightProducerSettings = ProducerSettings(
    system,
    flightKeySerde.serializer,
    flightRawSerde.serializer
  )

  val flightClient = new FlightClient()

  logger.info("Going to start consuming Meetup streams")

  val source: Source[HttpRequest, Cancellable] =
    Source.tick(0.seconds, 30.seconds, HttpRequest(HttpMethods.GET, config.aviation.flightStream.getAviationUri()))

  val flightSinkFactory = new KafkaSinkFactory[FlightMessageJson, Flight.Key, Flight.Value](
    config.kafka.flightRawTopic,
    flightProducerSettings
  )

  source
    .via(flightClient.flightRequest) //Future[List[FlightMessageJson]
    .mapAsync(4)(identity)
    .mapConcat(identity) // FlightMessageJson
    .runWith(flightSinkFactory.sink)

  val routes        = new Routes(config.server)
  val bindingFuture = Http().bindAndHandle(routes.routes, host, port)

  bindingFuture.map { serverBinding =>
    logger.info(s"Exposing to ${serverBinding.localAddress}")
  }

  sys.addShutdownHook {
    logger.info("Shutting down")

    val resourcesClosed = for {
      binding <- bindingFuture
      _       <- binding.terminate(hardDeadline = 3.seconds)
      t       <- system.terminate()
    } yield t

    Await.result(resourcesClosed, 10.seconds)
  }

}
