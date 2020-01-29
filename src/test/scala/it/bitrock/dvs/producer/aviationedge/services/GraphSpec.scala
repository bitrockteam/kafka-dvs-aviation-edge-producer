package it.bitrock.dvs.producer.aviationedge.services

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaSinkFactory
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{Error, Flight}
import it.bitrock.dvs.producer.aviationedge.model.{ErrorMessageJson, MessageJson}
import it.bitrock.testcommons.{FixtureLoanerAnyResult, Suite}
import net.manub.embeddedkafka.schemaregistry._
import org.apache.kafka.common.serialization.{Deserializer, Serdes}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class GraphSpec
    extends TestKit(ActorSystem("GraphSpec"))
    with Suite
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with EmbeddedKafka
    with TestValues {

  import GraphSpec._
  import MainFunctions.buildGraph

  "graph method" should {

    "routes error messages and correct messages to different topics" in ResourceLoaner.withFixture {
      case Resource(embeddedKafkaConfig, flightFactory, errorFactory) =>
        implicit val embKafkaConfig: EmbeddedKafkaConfig   = embeddedKafkaConfig
        implicit val keyDeserializer: Deserializer[String] = Serdes.String.deserializer

        val source = Source(List(Right(FlightMessage), Left(ErrorMessage)))

        val result = withRunningKafka {
          buildGraph(source, flightFactory.sink, errorFactory.sink).run()
          val flight = consumeFirstKeyedMessageFrom[String, Flight.Value](flightFactory.topic)._2
          val error  = consumeFirstKeyedMessageFrom[String, Error.Value](errorFactory.topic)._2
          (flight, error)
        }

        result._1 shouldBe ExpectedFlightRaw
        result._2 shouldBe ExpectedParserErrorMessage

    }

  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      implicit val embeddedKafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig()

      val flightTopic      = "flight_topic"
      val errorTopic       = "error_topic"
      val flightSerializer = specificAvroValueSerializer[Flight.Value]
      val errorSerializer  = specificAvroValueSerializer[Error.Value]

      val flightProducerSettings = getLocalSetting(ProducerSettings(system, Serdes.String.serializer, flightSerializer))
      val errorProducerSettings  = getLocalSetting(ProducerSettings(system, Serdes.String.serializer, errorSerializer))

      val flightFactory = new KafkaSinkFactory[MessageJson, String, Flight.Value](flightTopic, flightProducerSettings)
      val errorFactory  = new KafkaSinkFactory[ErrorMessageJson, String, Error.Value](errorTopic, errorProducerSettings)

      body(
        Resource(
          embeddedKafkaConfig,
          flightFactory,
          errorFactory
        )
      )

    }

    def getLocalSetting[K, V](
        producerSettings: ProducerSettings[K, V]
    )(implicit embeddedKafkaConfig: EmbeddedKafkaConfig): ProducerSettings[K, V] =
      producerSettings
        .withBootstrapServers(s"localhost:${embeddedKafkaConfig.kafkaPort}")
        .withProperty(
          AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
          s"http://localhost:${embeddedKafkaConfig.schemaRegistryPort}"
        )

  }

  override def afterAll(): Unit = {
    shutdown()
    super.afterAll()
  }

}

object GraphSpec {

  final case class Resource(
      embeddedKafkaConfig: EmbeddedKafkaConfig,
      flightFactory: KafkaSinkFactory[MessageJson, String, Flight.Value],
      errorFactory: KafkaSinkFactory[ErrorMessageJson, String, Error.Value]
  )

}
