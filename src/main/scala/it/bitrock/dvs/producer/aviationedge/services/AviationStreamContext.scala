package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Sink
import it.bitrock.dvs.producer.aviationedge.config.{AviationConfig, AviationStreamConfig, KafkaConfig}
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaSinkFactory
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes._
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.kafkacommons.serialization.AvroSerdes
import org.apache.kafka.common.serialization.Serdes
import it.bitrock.dvs.producer.aviationedge.kafka.InterceptorConfig._

import scala.concurrent.Future

trait AviationStreamContext[A] {
  def config(aviationConfig: AviationConfig): AviationStreamConfig
  def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]]
}

object AviationStreamContext {
  def apply[A](implicit streamContext: AviationStreamContext[A]): AviationStreamContext[A] = streamContext

  implicit val FlightStreamContext: AviationStreamContext[FlightStream.type] = new AviationStreamContext[FlightStream.type] {
    override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.flightStream

    override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
      val flightRawSerializer = AvroSerdes.serdeFrom[Flight.Value](kafkaConfig.schemaRegistryUrl).serializer
      val flightProducerSettings =
        ProducerSettings(system, Serdes.String().serializer, flightRawSerializer).withInterceptorConfig(kafkaConfig)
      new KafkaSinkFactory[MessageJson, Key, Flight.Value](kafkaConfig.flightRawTopic, flightProducerSettings).sink
    }
  }

  implicit val AirplaneStreamContext: AviationStreamContext[AirplaneStream.type] =
    new AviationStreamContext[AirplaneStream.type] {
      override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.airplaneStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
        val airplaneRawSerializer = AvroSerdes.serdeFrom[Airplane.Value](kafkaConfig.schemaRegistryUrl).serializer
        val airplaneProducerSettings =
          ProducerSettings(system, Serdes.String().serializer, airplaneRawSerializer).withInterceptorConfig(kafkaConfig)
        new KafkaSinkFactory[MessageJson, Key, Airplane.Value](kafkaConfig.airplaneRawTopic, airplaneProducerSettings).sink
      }
    }

  implicit val AirportStreamContext: AviationStreamContext[AirportStream.type] = new AviationStreamContext[AirportStream.type] {
    override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.airportStream

    override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
      val airportRawSerializer = AvroSerdes.serdeFrom[Airport.Value](kafkaConfig.schemaRegistryUrl).serializer
      val airportProducerSettings =
        ProducerSettings(system, Serdes.String().serializer, airportRawSerializer).withInterceptorConfig(kafkaConfig)
      new KafkaSinkFactory[MessageJson, Key, Airport.Value](kafkaConfig.airportRawTopic, airportProducerSettings).sink
    }
  }

  implicit val AirlineStreamContext: AviationStreamContext[AirlineStream.type] = new AviationStreamContext[AirlineStream.type] {
    override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.airlineStream

    override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
      val airlineRawSerializer = AvroSerdes.serdeFrom[Airline.Value](kafkaConfig.schemaRegistryUrl).serializer
      val airlineProducerSettings =
        ProducerSettings(system, Serdes.String().serializer, airlineRawSerializer).withInterceptorConfig(kafkaConfig)
      new KafkaSinkFactory[MessageJson, Key, Airline.Value](kafkaConfig.airlineRawTopic, airlineProducerSettings).sink
    }
  }

  implicit val CityStreamContext: AviationStreamContext[CityStream.type] = new AviationStreamContext[CityStream.type] {
    override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.cityStream

    override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
      val cityRawSerializer = AvroSerdes.serdeFrom[City.Value](kafkaConfig.schemaRegistryUrl).serializer
      val cityProducerSettings =
        ProducerSettings(system, Serdes.String().serializer, cityRawSerializer).withInterceptorConfig(kafkaConfig)
      new KafkaSinkFactory[MessageJson, Key, City.Value](kafkaConfig.cityRawTopic, cityProducerSettings).sink
    }
  }
}
