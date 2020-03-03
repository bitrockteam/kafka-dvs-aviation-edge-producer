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

  implicit val FlightStreamContext: ApiProviderStreamContext[FlightMessageJson] =
    new ApiProviderStreamContext[FlightMessageJson] {
      override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.flightStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[FlightMessageJson, Future[Done]] = {
        val flightProducerSettings = ProducerSettingsFactory.from[Flight.Value](kafkaConfig)
        new KafkaSinkFactory[FlightMessageJson, Key, Flight.Value](kafkaConfig.flightRawTopic, flightProducerSettings).sink
      }
    }

  implicit val AirplaneStreamContext: ApiProviderStreamContext[AirplaneMessageJson] =
    new ApiProviderStreamContext[AirplaneMessageJson] {
      override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.airplaneStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[AirplaneMessageJson, Future[Done]] = {
        val airplaneProducerSettings = ProducerSettingsFactory.from[Airplane.Value](kafkaConfig)
        new KafkaSinkFactory[AirplaneMessageJson, Key, Airplane.Value](kafkaConfig.airplaneRawTopic, airplaneProducerSettings).sink
      }
    }

  implicit val AirportStreamContext: ApiProviderStreamContext[AirportMessageJson] =
    new ApiProviderStreamContext[AirportMessageJson] {
      override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.airportStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[AirportMessageJson, Future[Done]] = {
        val airportProducerSettings = ProducerSettingsFactory.from[Airport.Value](kafkaConfig)
        new KafkaSinkFactory[AirportMessageJson, Key, Airport.Value](kafkaConfig.airportRawTopic, airportProducerSettings).sink
      }
    }

  implicit val AirlineStreamContext: ApiProviderStreamContext[AirlineMessageJson] =
    new ApiProviderStreamContext[AirlineMessageJson] {
      override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.airlineStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[AirlineMessageJson, Future[Done]] = {
        val airlineProducerSettings = ProducerSettingsFactory.from[Airline.Value](kafkaConfig)
        new KafkaSinkFactory[AirlineMessageJson, Key, Airline.Value](kafkaConfig.airlineRawTopic, airlineProducerSettings).sink
      }
    }

  implicit val CityStreamContext: ApiProviderStreamContext[CityMessageJson] = new ApiProviderStreamContext[CityMessageJson] {
    override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.aviationEdge.cityStream

    override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[CityMessageJson, Future[Done]] = {
      val cityProducerSettings = ProducerSettingsFactory.from[City.Value](kafkaConfig)
      new KafkaSinkFactory[CityMessageJson, Key, City.Value](kafkaConfig.cityRawTopic, cityProducerSettings).sink
    }
  }
}
