package it.bitrock.dvs.producer.aviationedge.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.Flight.Value
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{Key, Flight => KafkaTypesFlight}
import it.bitrock.dvs.producer.aviationedge.model.MessageJson
import it.bitrock.kafkacommons.serialization.ImplicitConversions._
import it.bitrock.testcommons.Suite
import net.manub.embeddedkafka.schemaregistry.EmbeddedKafka._
import net.manub.embeddedkafka.schemaregistry._
import org.apache.kafka.common.serialization.{Serde, Serdes, Serializer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class KafkaFlightSinkFactorySpec
    extends TestKit(ActorSystem("KafkaFlightSinkFactorySpec"))
    with Suite
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with TestValues {

  "sink method" should {

    "convert a domain model to Kafka model and push it to a topic" in {
      val embeddedKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = 0, zooKeeperPort = 0, schemaRegistryPort = 0)
      val outputTopic                              = "output_topic"

      implicit val keySerde: Serde[Key] = Serdes.String

      val result = withRunningKafkaOnFoundPort(embeddedKafkaConfig) { implicit conf =>
        implicit val valueSerializer: Serializer[Value] = specificAvroValueSerializer[KafkaTypesFlight.Value]
        val producerSettings = ProducerSettings(system, keySerde.serializer, valueSerializer)
          .withBootstrapServers(s"localhost:${conf.kafkaPort}")
          .withProperty(
            AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
            s"http://localhost:${conf.schemaRegistryPort}"
          )

        val factory = new KafkaSinkFactory[MessageJson, Key, KafkaTypesFlight.Value](
          outputTopic,
          producerSettings
        )

        Source.single(FlightMessage).runWith(factory.sink)
        consumeFirstKeyedMessageFrom[Key, KafkaTypesFlight.Value](factory.topic)
      }
      result shouldBe ((IcaoNumber, ExpectedFlightRaw))
    }

  }

  override def afterAll(): Unit = {
    shutdown()
    super.afterAll()
  }

}
