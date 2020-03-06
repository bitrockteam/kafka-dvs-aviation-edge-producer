package it.bitrock.dvs.producer.aviationedge.kafka

import KafkaErrorSinkFactorySpec._
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import it.bitrock.dvs.producer.aviationedge.TestValues._
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{Error, Key}
import it.bitrock.dvs.producer.aviationedge.model.ErrorMessageJson
import it.bitrock.kafkacommons.serialization.ImplicitConversions._
import it.bitrock.testcommons.{FixtureLoanerAnyResult, Suite}
import net.manub.embeddedkafka.schemaregistry._
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class KafkaErrorSinkFactorySpec
    extends TestKit(ActorSystem("KafkaErrorSinkFactorySpec"))
    with Suite
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with EmbeddedKafka {

  "sink method" should {
    "convert a domain model to Kafka model and push it to a topic" in ResourceLoaner.withFixture {
      case Resource(embeddedKafkaConfig, keySerde, factory) =>
        implicit val embKafkaConfig: EmbeddedKafkaConfig = embeddedKafkaConfig
        implicit val kSerde: Serde[Key]                  = keySerde
        val result = withRunningKafka {
          Source.single(ErrorMessage).runWith(factory.sink)
          consumeFirstKeyedMessageFrom[Key, Error.Value](factory.topic)._2
        }
        result shouldBe ExpectedParserError
    }
  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      implicit val embeddedKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig()
      val outputTopic                                       = "output_topic"
      val keySerde                                          = Serdes.String
      val valueSerializer                                   = specificAvroValueSerializer[Error.Value]

      val producerSettings = ProducerSettings(system, keySerde.serializer, valueSerializer)
        .withBootstrapServers(s"localhost:${embeddedKafkaConfig.kafkaPort}")
        .withProperty(
          AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
          s"http://localhost:${embeddedKafkaConfig.schemaRegistryPort}"
        )

      val factory = new KafkaSinkFactory[ErrorMessageJson, Key, Error.Value](
        outputTopic,
        producerSettings
      )

      body(
        Resource(
          embeddedKafkaConfig,
          keySerde,
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

object KafkaErrorSinkFactorySpec {
  final case class Resource(
      embeddedKafkaConfig: EmbeddedKafkaConfig,
      keySerde: Serde[Key],
      factory: KafkaSinkFactory[ErrorMessageJson, Key, Error.Value]
  )
}
