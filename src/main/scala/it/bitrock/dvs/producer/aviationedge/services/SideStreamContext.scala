package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Sink
import it.bitrock.dvs.producer.aviationedge.config.KafkaConfig
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaSinkFactory
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{Error, Key, Monitoring}
import it.bitrock.dvs.producer.aviationedge.model.{ErrorMessageJson, MonitoringMessageJson}
import it.bitrock.kafkacommons.serialization.AvroSerdes
import org.apache.kafka.common.serialization.Serdes

import scala.concurrent.Future

object SideStreamContext {

  def errorSink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[ErrorMessageJson, Future[Done]] = {
    val errorSerializer       = AvroSerdes.serdeFrom[Error.Value](kafkaConfig.schemaRegistryUrl).serializer
    val errorProducerSettings = ProducerSettings(system, Serdes.String().serializer, errorSerializer)
    new KafkaSinkFactory[ErrorMessageJson, Key, Error.Value](kafkaConfig.parserErrorTopic, errorProducerSettings).sink
  }

  def monitoringSink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MonitoringMessageJson, Future[Done]] = {
    val monitoringSerializer       = AvroSerdes.serdeFrom[Monitoring.Value](kafkaConfig.schemaRegistryUrl).serializer
    val monitoringProducerSettings = ProducerSettings(system, Serdes.String().serializer, monitoringSerializer)
    new KafkaSinkFactory[MonitoringMessageJson, Key, Monitoring.Value](kafkaConfig.monitoringTopic, monitoringProducerSettings).sink
  }

}
