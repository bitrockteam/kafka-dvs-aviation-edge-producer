package it.bitrock.dvs.producer.aviationedge.services.context

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import it.bitrock.dvs.producer.aviationedge.config.{ApiProviderConfig, ApiProviderStreamConfig, KafkaConfig}
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{FlightState, Key}
import it.bitrock.dvs.producer.aviationedge.kafka.{KafkaSinkFactory, ProducerSettingsFactory}
import it.bitrock.dvs.producer.aviationedge.model.FlightStateJson

import scala.concurrent.Future

object OpenSkyStreamContext {
  def apply[A](implicit streamContext: ApiProviderStreamContext[A]): ApiProviderStreamContext[A] = streamContext

  implicit val FlightStateStreamContext: ApiProviderStreamContext[FlightStateJson] =
    new ApiProviderStreamContext[FlightStateJson] {
      override def config(aviationConfig: ApiProviderConfig): ApiProviderStreamConfig = aviationConfig.openSky.flightStateStream

      override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[FlightStateJson, Future[Done]] = {
        val flightProducerSettings = ProducerSettingsFactory.from[FlightState.Value](kafkaConfig)
        new KafkaSinkFactory[FlightStateJson, Key, FlightState.Value](kafkaConfig.flightOpenSkyRawTopic, flightProducerSettings).sink
      }
    }
}
