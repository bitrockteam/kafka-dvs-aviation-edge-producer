package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import it.bitrock.dvs.producer.aviationedge.config.KafkaConfig
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{Error, Flight, Key, Monitoring}
import it.bitrock.dvs.producer.aviationedge.kafka.{KafkaSinkFactory, ProducerSettingsFactory}
import it.bitrock.dvs.producer.aviationedge.model.{ErrorMessageJson, FlightMessageJson, MonitoringMessageJson}

import scala.concurrent.Future

object SideStreamContext {
  def errorSink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[ErrorMessageJson, Future[Done]] = {
    val producerSettings = ProducerSettingsFactory.from[Error.Value](kafkaConfig)
    new KafkaSinkFactory[ErrorMessageJson, Key, Error.Value](kafkaConfig.parserErrorTopic, producerSettings).sink
  }

  def monitoringSink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MonitoringMessageJson, Future[Done]] = {
    val producerSettings = ProducerSettingsFactory.from[Monitoring.Value](kafkaConfig)
    new KafkaSinkFactory[MonitoringMessageJson, Key, Monitoring.Value](kafkaConfig.monitoringTopic, producerSettings).sink
  }

  def invalidFlightSink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[FlightMessageJson, Future[Done]] = {
    val producerSettings = ProducerSettingsFactory.from[Flight.Value](kafkaConfig)
    new KafkaSinkFactory[FlightMessageJson, Key, Flight.Value](kafkaConfig.invalidFlightRawTopic, producerSettings).sink
  }
}
