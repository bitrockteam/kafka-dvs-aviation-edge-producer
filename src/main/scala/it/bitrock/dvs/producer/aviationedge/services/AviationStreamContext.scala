package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import it.bitrock.dvs.producer.aviationedge.config.{AviationConfig, AviationStreamConfig, KafkaConfig}
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes._
import it.bitrock.dvs.producer.aviationedge.kafka.{KafkaSinkFactory, ProducerSettingsFactory}
import it.bitrock.dvs.producer.aviationedge.model._

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
      val flightProducerSettings = ProducerSettingsFactory.from[Flight.Value](kafkaConfig)
      new KafkaSinkFactory[MessageJson, Key, Flight.Value](kafkaConfig.flightRawTopic, flightProducerSettings).sink
    }
  }

  implicit val AirplaneStreamContext: AviationStreamContext[AirplaneStream.type] =
    new AviationStreamContext[AirplaneStream.type] {
      override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.airplaneStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
        val airplaneProducerSettings = ProducerSettingsFactory.from[Airplane.Value](kafkaConfig)
        new KafkaSinkFactory[MessageJson, Key, Airplane.Value](kafkaConfig.airplaneRawTopic, airplaneProducerSettings).sink
      }
    }

  implicit val AirportStreamContext: AviationStreamContext[AirportStream.type] = new AviationStreamContext[AirportStream.type] {
    override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.airportStream

    override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
      val airportProducerSettings = ProducerSettingsFactory.from[Airport.Value](kafkaConfig)
      new KafkaSinkFactory[MessageJson, Key, Airport.Value](kafkaConfig.airportRawTopic, airportProducerSettings).sink
    }
  }

  implicit val AirlineStreamContext: AviationStreamContext[AirlineStream.type] = new AviationStreamContext[AirlineStream.type] {
    override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.airlineStream

    override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
      val airlineProducerSettings = ProducerSettingsFactory.from[Airline.Value](kafkaConfig)
      new KafkaSinkFactory[MessageJson, Key, Airline.Value](kafkaConfig.airlineRawTopic, airlineProducerSettings).sink
    }
  }

  implicit val CityStreamContext: AviationStreamContext[CityStream.type] = new AviationStreamContext[CityStream.type] {
    override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.cityStream

    override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
      val cityProducerSettings = ProducerSettingsFactory.from[City.Value](kafkaConfig)
      new KafkaSinkFactory[MessageJson, Key, City.Value](kafkaConfig.cityRawTopic, cityProducerSettings).sink
    }
  }
}
