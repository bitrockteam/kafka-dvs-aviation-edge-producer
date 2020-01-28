package it.bitrock.dvs.producer.aviationedge.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.Error
import it.bitrock.dvs.producer.aviationedge.model.ErrorMessageJson
import it.bitrock.testcommons.{FixtureLoanerAnyResult, Suite}
import net.manub.embeddedkafka.schemaregistry._
import org.apache.kafka.common.serialization.{Deserializer, Serdes}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class KafkaErrorSinkFactorySpec
    extends TestKit(ActorSystem("ErrorHandlerSpec"))
    with Suite
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with EmbeddedKafka
    with TestValues {

  import KafkaErrorSinkFactorySpec._

  "sink method" should {

    "convert a domain model to Kafka model and push it to a topic" in ResourceLoaner.withFixture {
      case Resource(embeddedKafkaConfig, factory) =>
        implicit val embKafkaConfig: EmbeddedKafkaConfig   = embeddedKafkaConfig
        implicit val keyDeserializer: Deserializer[String] = Serdes.String.deserializer
        val result = withRunningKafka {
          Source.single(ErrorMessage).runWith(factory.sink)
          consumeFirstKeyedMessageFrom[String, Error.Value](factory.topic)
        }
        result shouldBe ((null, ExpectedParserErrorMessage))
    }

  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      implicit val embeddedKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig()
      val outputTopic                                       = "output_topic"
      val valueSerializer                                   = specificAvroValueSerializer[Error.Value]

      val producerSettings = ProducerSettings(system, Serdes.String.serializer, valueSerializer)
        .withBootstrapServers(s"localhost:${embeddedKafkaConfig.kafkaPort}")
        .withProperty(
          AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
          s"http://localhost:${embeddedKafkaConfig.schemaRegistryPort}"
        )

      val factory = new KafkaSinkFactory[ErrorMessageJson, String, Error.Value](
        outputTopic,
        producerSettings
      )

      body(
        Resource(
          embeddedKafkaConfig,
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
      factory: KafkaSinkFactory[ErrorMessageJson, String, Error.Value]
  )

}
