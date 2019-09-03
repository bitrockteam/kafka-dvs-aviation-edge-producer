package it.bitrock.kafkaflightstream.producer.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import it.bitrock.kafkaflightstream.model._
import it.bitrock.kafkaflightstream.producer.kafka.KafkaTypes.{Flight => KafkaTypesFlight}
import it.bitrock.kafkaflightstream.producer.model._
import it.bitrock.kafkageostream.kafkacommons.serialization.ImplicitConversions._
import it.bitrock.kafkageostream.testcommons.{FixtureLoanerAnyResult, Suite}
import net.manub.embeddedkafka.schemaregistry.{EmbeddedKafka, EmbeddedKafkaConfig, _}
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class KafkaFlightSinkFactorySpec
    extends TestKit(ActorSystem("KafkaFlightSinkFactorySpec"))
    with Suite
    with WordSpecLike
    with BeforeAndAfterAll
    with EmbeddedKafka {
  import KafkaFlightSinkFactorySpec._

  implicit val mat: ActorMaterializer = ActorMaterializer()

  "sink method" should {

    "convert a domain model to Kafka model and push it to a topic" in ResourceLoaner.withFixture {
      case Resource(embeddedKafkaConfig, keySerde, factory) =>
        implicit val embKafkaConfig: EmbeddedKafkaConfig = embeddedKafkaConfig
        implicit val kSerde: Serde[KafkaTypesFlight.Key] = keySerde

        val message = FlightMessageJson(
          GeographyJson(
            49.2655,
            -1.9623,
            9753.6,
            282.76
          ),
          SpeedJson(
            805.14,
            0
          ),
          CommonCodeJson(
            "ZRH",
            "LSZH"
          ),
          CommonCodeJson(
            "ORD",
            "KORD"
          ),
          AircraftJson(
            "HBJHA",
            "A333",
            "",
            "A333"
          ),
          CommonCodeJson(
            "LX",
            "SWR"
          ),
          FlightJson(
            "LX6U",
            "SWR6U",
            "6U"
          ),
          SystemJson(
            "1567415880",
            "3061"
          ),
          "en-route"
        )

        val result = withRunningKafka {
          Source.single(message).runWith(factory.sink)

          consumeFirstKeyedMessageFrom[KafkaTypesFlight.Key, KafkaTypesFlight.Value](factory.topic)
        }

        val expectedValue = FlightRaw(
          Geography(
            message.geography.latitude,
            message.geography.longitude,
            message.geography.altitude,
            message.geography.direction
          ),
          Speed(
            message.speed.horizontal,
            message.speed.vertical
          ),
          CommonCode(
            message.departure.iataCode,
            message.departure.icaoCode
          ),
          CommonCode(
            message.arrival.iataCode,
            message.arrival.icaoCode
          ),
          Aircraft(
            message.aircraft.regNumber,
            message.aircraft.icaoCode,
            message.aircraft.icao24,
            message.aircraft.iataCode
          ),
          CommonCode(
            message.airline.iataCode,
            message.airline.icaoCode
          ),
          Flight(
            message.flight.iataNumber,
            message.flight.icaoNumber,
            message.flight.number
          ),
          System(
            message.system.updated,
            message.system.squawk
          ),
          message.status
        )

        result shouldBe (message.flight.iataNumber, expectedValue)
    }

  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      implicit val embeddedKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig()

      val outputTopic     = "flight_received"
      val rsvpRawKeySerde = Serdes.String
      val rsvpRawSer      = specificAvroSerializer[KafkaTypesFlight.Value]

      val producerSettings = ProducerSettings(system, rsvpRawKeySerde.serializer, rsvpRawSer)
        .withBootstrapServers(s"localhost:${embeddedKafkaConfig.kafkaPort}")
        .withProperty(
          AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
          s"http://localhost:${embeddedKafkaConfig.schemaRegistryPort}"
        )

      val factory = new KafkaSinkFactory[FlightMessageJson, KafkaTypesFlight.Key, KafkaTypesFlight.Value](
        outputTopic,
        producerSettings
      )

      body(
        Resource(
          embeddedKafkaConfig,
          rsvpRawKeySerde,
          factory
        )
      )
    }
  }

  override def afterAll(): Unit = {
    shutdown()
    super.afterAll()
  }

}

object KafkaFlightSinkFactorySpec {

  final case class Resource(
      embeddedKafkaConfig: EmbeddedKafkaConfig,
      keySerde: Serde[KafkaTypesFlight.Key],
      factory: KafkaSinkFactory[FlightMessageJson, KafkaTypesFlight.Key, KafkaTypesFlight.Value]
  )

}
