package it.bitrock.dvs.producer.aviationedge.kafka

import akka.kafka.ProducerSettings
import it.bitrock.dvs.producer.aviationedge.config.KafkaConfig
import org.apache.kafka.clients.producer.ProducerConfig

object InterceptorConfig {

  final private val InterceptorSettingKey   = ProducerConfig.INTERCEPTOR_CLASSES_CONFIG
  final private val InterceptorSettingValue = "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor"

  implicit class ProducerSettingOps[K, V](producerSettings: ProducerSettings[K, V]) {

    def withInterceptorConfig(kafkaConfig: KafkaConfig): ProducerSettings[K, V] =
      if (kafkaConfig.enableInterceptors)
        producerSettings.withProperties(InterceptorSettingKey -> InterceptorSettingValue)
      else
        producerSettings
  }
}
