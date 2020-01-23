package it.bitrock.dvs.producer.aviationedge.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.model.MessageJson
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{Key, Flight => KafkaTypesFlight}
import it.bitrock.kafkacommons.serialization.ImplicitConversions._
import it.bitrock.testcommons.{FixtureLoanerAnyResult, Suite}
import net.manub.embeddedkafka.schemaregistry._
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class KafkaFlightSinkFactorySpec
    extends TestKit(ActorSystem("KafkaFlightSinkFactorySpec"))
    with Suite
    with WordSpecLike
    with BeforeAndAfterAll
    with EmbeddedKafka
    with TestValues {
  import KafkaFlightSinkFactorySpec._

  "sink method" should {

    "convert a domain model to Kafka model and push it to a topic" in ResourceLoaner.withFixture {
      case Resource(embeddedKafkaConfig, keySerde, factory) =>
        implicit val embKafkaConfig: EmbeddedKafkaConfig = embeddedKafkaConfig
        implicit val kSerde: Serde[Key]                  = keySerde
        val result = withRunningKafka {
          Source.single(FlightMessage).runWith(factory.sink)
          consumeFirstKeyedMessageFrom[Key, KafkaTypesFlight.Value](factory.topic)
        }
        result shouldBe ((IcaoNumber, ExpectedFlightRaw))
    }

  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      implicit val embeddedKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig()

      val outputTopic       = "output_topic"
      val rsvpRawKeySerde   = Serdes.String
      val rsvpRawSerializer = specificAvroValueSerializer[KafkaTypesFlight.Value]

      val producerSettings = ProducerSettings(system, rsvpRawKeySerde.serializer, rsvpRawSerializer)
        .withBootstrapServers(s"localhost:${embeddedKafkaConfig.kafkaPort}")
        .withProperty(
          AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
          s"http://localhost:${embeddedKafkaConfig.schemaRegistryPort}"
        )

      val factory = new KafkaSinkFactory[MessageJson, Key, KafkaTypesFlight.Value](
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
      keySerde: Serde[Key],
      factory: KafkaSinkFactory[MessageJson, Key, KafkaTypesFlight.Value]
  )

}
