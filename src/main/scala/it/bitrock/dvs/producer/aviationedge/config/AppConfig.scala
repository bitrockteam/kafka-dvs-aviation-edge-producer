package it.bitrock.dvs.producer.aviationedge.config

import pureconfig.ConfigSource
import pureconfig.generic.auto._

final case class AppConfig(
    kafka: KafkaConfig,
    server: ServerConfig,
    apiProvider: ApiProviderConfig
)

object AppConfig {
  val config: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

  def server: ServerConfig           = config.server
  def apiProvider: ApiProviderConfig = config.apiProvider
  def kafka: KafkaConfig             = config.kafka
}
