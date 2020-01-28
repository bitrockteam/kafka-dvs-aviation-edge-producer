package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Sink
import it.bitrock.dvs.producer.aviationedge.config.KafkaConfig
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaSinkFactory
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.Error
import it.bitrock.dvs.producer.aviationedge.model.ErrorMessageJson
import it.bitrock.kafkacommons.serialization.AvroSerdes
import org.apache.kafka.common.serialization.Serdes

import scala.concurrent.Future

object ErrorStreamContext {

  def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[ErrorMessageJson, Future[Done]] = {
    val errorSerializer       = AvroSerdes.serdeFrom[Error.Value](kafkaConfig.schemaRegistryUrl).serializer
    val errorProducerSettings = ProducerSettings(system, Serdes.String().serializer, errorSerializer)
    new KafkaSinkFactory[ErrorMessageJson, String, Error.Value](kafkaConfig.parserErrorTopic, errorProducerSettings).sink
  }

}
