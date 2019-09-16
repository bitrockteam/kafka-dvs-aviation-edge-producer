package it.bitrock.kafkaflightstream.producer.config

import pureconfig.generic.auto._

final case class AppConfig(
    kafka: KafkaConfig,
    server: ServerConfig,
    aviation: AviationConfig
)

object AppConfig {

  def load: AppConfig = pureconfig.loadConfigOrThrow[AppConfig]

}
