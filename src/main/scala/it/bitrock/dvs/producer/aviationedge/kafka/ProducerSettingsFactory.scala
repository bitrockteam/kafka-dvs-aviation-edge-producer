package it.bitrock.dvs.producer.aviationedge.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import it.bitrock.dvs.producer.aviationedge.config.KafkaConfig
import it.bitrock.kafkacommons.serialization.AvroSerdes
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes

object ProducerSettingsFactory {
  final private val InterceptorSettingKey   = ProducerConfig.INTERCEPTOR_CLASSES_CONFIG
  final private val InterceptorSettingValue = "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor"

  def from[V <: SpecificRecord](
      kafkaConfig: KafkaConfig
  )(implicit system: ActorSystem): ProducerSettings[String, V] = {
    val valueSerializer  = AvroSerdes.serdeFrom[V](kafkaConfig.schemaRegistryUrl).serializer
    val keySerializer    = Serdes.String().serializer
    val producerSettings = ProducerSettings(system, keySerializer, valueSerializer)
    if (kafkaConfig.enableInterceptors)
      producerSettings.withProperties(InterceptorSettingKey -> InterceptorSettingValue)
    else
      producerSettings
  }
}
