package it.bitrock.dvs.producer.config

import pureconfig.generic.auto._

final case class AppConfig(
    kafka: KafkaConfig,
    server: ServerConfig,
    aviation: AviationConfig
)

object AppConfig {

  val config: AppConfig = pureconfig.loadConfigOrThrow[AppConfig]

  def server: ServerConfig     = config.server
  def aviation: AviationConfig = config.aviation
  def kafka: KafkaConfig       = config.kafka

}
