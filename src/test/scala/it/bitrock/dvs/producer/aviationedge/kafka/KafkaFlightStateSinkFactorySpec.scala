package it.bitrock.dvs.producer.aviationedge.kafka

import KafkaFlightStateSinkFactorySpec._
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import it.bitrock.dvs.producer.aviationedge.TestValues._
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{FlightState, Key}
import it.bitrock.dvs.producer.aviationedge.model.MessageJson
import it.bitrock.kafkacommons.serialization.ImplicitConversions._
import it.bitrock.testcommons.{FixtureLoanerAnyResult, Suite}
import net.manub.embeddedkafka.schemaregistry._
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class KafkaFlightStateSinkFactorySpec
    extends TestKit(ActorSystem("KafkaFlightSinkFactorySpec"))
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
          Source.single(FlightStateMessage).runWith(factory.sink)
          consumeFirstKeyedMessageFrom[Key, FlightState.Value](factory.topic)
        }
        result shouldBe ((IcaoNumber, ExpectedFlightStateRaw))
    }
  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      implicit val embeddedKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig()
      val outputTopic                                       = "output_topic"
      val keySerde                                          = Serdes.String
      val valueSerializer                                   = specificAvroValueSerializer[FlightState.Value]

      val producerSettings = ProducerSettings(system, keySerde.serializer, valueSerializer)
        .withBootstrapServers(s"localhost:${embeddedKafkaConfig.kafkaPort}")
        .withProperty(
          AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
          s"http://localhost:${embeddedKafkaConfig.schemaRegistryPort}"
        )

      val factory = new KafkaSinkFactory[MessageJson, Key, FlightState.Value](
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

object KafkaFlightStateSinkFactorySpec {
  final case class Resource(
      embeddedKafkaConfig: EmbeddedKafkaConfig,
      keySerde: Serde[Key],
      factory: KafkaSinkFactory[MessageJson, Key, FlightState.Value]
  )
}
