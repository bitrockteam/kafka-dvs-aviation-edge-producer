package it.bitrock.kafkaflightstream.producer.config

final case class ServerConfig(
    host: String,
    port: Int,
    rest: RestConfig
)

final case class RestConfig(
    healthPath: String
)
