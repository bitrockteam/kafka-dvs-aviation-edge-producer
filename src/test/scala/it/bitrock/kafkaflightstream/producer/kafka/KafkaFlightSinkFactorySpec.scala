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
            1548891000000L,
            1548891000000L,
            1548891000000L,
            1548891000000L
          ),
          SpeedJson(
            1548891000000L,
            1548891000000L
          ),
          CommonCodeJson(
            "feg",
            "gsh"
          ),
          CommonCodeJson(
            "feg",
            "gsh"
          ),
          AircraftJson(
            "feg",
            "gsh",
            "feg",
            "gsh"
          ),
          CommonCodeJson(
            "feg",
            "gsh"
          ),
          FlightJson(
            "gdgf",
            "gsdgd",
            "gsdgds"
          ),
          SystemJson(
            "fgdsf",
            "ghdhd"
          ),
          "dotnet-Sao-Paulo"
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
            message.departure.iata_code,
            message.departure.icao_code
          ),
          CommonCode(
            message.arrival.iata_code,
            message.arrival.icao_code
          ),
          Aircraft(
            message.aircraft.reg_number,
            message.aircraft.icao_code,
            message.aircraft.icao24,
            message.aircraft.iata_code
          ),
          CommonCode(
            message.airline.iata_code,
            message.airline.icao_code
          ),
          Flight(
            message.flight.iata_number,
            message.flight.icao_number,
            message.flight.number
          ),
          System(
            message.system.updated,
            message.system.squawk
          ),
          message.status
        )

        result shouldBe (message.flight.iata_number, expectedValue)
    }

  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      implicit val embeddedKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig()

      val outputTopic     = "rsvp_received"
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
