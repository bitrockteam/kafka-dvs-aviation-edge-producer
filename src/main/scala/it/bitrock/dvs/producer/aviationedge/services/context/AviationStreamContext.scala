package it.bitrock.dvs.producer.aviationedge.services.context

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import it.bitrock.dvs.producer.aviationedge.config.{ApiProviderConfig, ApiProviderStreamConfig, KafkaConfig}
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes._
import it.bitrock.dvs.producer.aviationedge.kafka.{KafkaSinkFactory, ProducerSettingsFactory}
import it.bitrock.dvs.producer.aviationedge.model._

import scala.concurrent.Future

object AviationStreamContext {
  def apply[A](implicit streamContext: ApiProviderStreamContext[A]): ApiProviderStreamContext[A] = streamContext

  implicit val FlightStreamContext: ApiProviderStreamContext[FlightStream.type] =
    new ApiProviderStreamContext[FlightStream.type] {
      override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.flightStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
        val flightProducerSettings = ProducerSettingsFactory.from[Flight.Value](kafkaConfig)
        new KafkaSinkFactory[MessageJson, Key, Flight.Value](kafkaConfig.flightRawTopic, flightProducerSettings).sink
      }
    }

  implicit val AirplaneStreamContext: ApiProviderStreamContext[AirplaneStream.type] =
    new ApiProviderStreamContext[AirplaneStream.type] {
      override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.airplaneStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
        val airplaneProducerSettings = ProducerSettingsFactory.from[Airplane.Value](kafkaConfig)
        new KafkaSinkFactory[MessageJson, Key, Airplane.Value](kafkaConfig.airplaneRawTopic, airplaneProducerSettings).sink
      }
    }

  implicit val AirportStreamContext: ApiProviderStreamContext[AirportStream.type] =
    new ApiProviderStreamContext[AirportStream.type] {
      override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.airportStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
        val airportProducerSettings = ProducerSettingsFactory.from[Airport.Value](kafkaConfig)
        new KafkaSinkFactory[MessageJson, Key, Airport.Value](kafkaConfig.airportRawTopic, airportProducerSettings).sink
      }
    }

  implicit val AirlineStreamContext: ApiProviderStreamContext[AirlineStream.type] =
    new ApiProviderStreamContext[AirlineStream.type] {
      override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.airlineStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
        val airlineProducerSettings = ProducerSettingsFactory.from[Airline.Value](kafkaConfig)
        new KafkaSinkFactory[MessageJson, Key, Airline.Value](kafkaConfig.airlineRawTopic, airlineProducerSettings).sink
      }
    }

  implicit val CityStreamContext: ApiProviderStreamContext[CityStream.type] = new ApiProviderStreamContext[CityStream.type] {
    override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.cityStream

    override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] = {
      val cityProducerSettings = ProducerSettingsFactory.from[City.Value](kafkaConfig)
      new KafkaSinkFactory[MessageJson, Key, City.Value](kafkaConfig.cityRawTopic, cityProducerSettings).sink
    }
  }
}
