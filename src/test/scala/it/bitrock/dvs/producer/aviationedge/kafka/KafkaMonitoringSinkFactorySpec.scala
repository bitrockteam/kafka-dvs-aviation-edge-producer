package it.bitrock.dvs.producer.aviationedge.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{Key, Monitoring}
import it.bitrock.dvs.producer.aviationedge.model.MonitoringMessageJson
import it.bitrock.kafkacommons.serialization.ImplicitConversions._
import it.bitrock.testcommons.{FixtureLoanerAnyResult, Suite}
import net.manub.embeddedkafka.schemaregistry._
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class KafkaMonitoringSinkFactorySpec
    extends TestKit(ActorSystem("KafkaMonitoringSinkFactorySpec"))
    with Suite
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with EmbeddedKafka
    with TestValues {
  import KafkaMonitoringSinkFactorySpec._

  "sink method" should {
    "convert a domain model to Kafka model and push it to a topic" in ResourceLoaner.withFixture {
      case Resource(embeddedKafkaConfig, keySerde, factory) =>
        implicit val embKafkaConfig: EmbeddedKafkaConfig = embeddedKafkaConfig
        implicit val kSerde: Serde[Key]                  = keySerde
        val result = withRunningKafka {
          Source.single(MonitoringMessage).runWith(factory.sink)
          consumeFirstKeyedMessageFrom[Key, Monitoring.Value](factory.topic)._2
        }
        result shouldBe ExpectedMonitoringMessage
    }
  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      implicit val embeddedKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig()
      val outputTopic                                       = "output_topic"
      val keySerde                                          = Serdes.String
      val valueSerializer                                   = specificAvroValueSerializer[Monitoring.Value]

      val producerSettings = ProducerSettings(system, keySerde.serializer, valueSerializer)
        .withBootstrapServers(s"localhost:${embeddedKafkaConfig.kafkaPort}")
        .withProperty(
          AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
          s"http://localhost:${embeddedKafkaConfig.schemaRegistryPort}"
        )

      val factory = new KafkaSinkFactory[MonitoringMessageJson, Key, Monitoring.Value](
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

object KafkaMonitoringSinkFactorySpec {
  final case class Resource(
      embeddedKafkaConfig: EmbeddedKafkaConfig,
      keySerde: Serde[Key],
      factory: KafkaSinkFactory[MonitoringMessageJson, Key, Monitoring.Value]
  )
}
