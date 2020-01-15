package it.bitrock.dvs.producer.config

import java.net.URI

final case class KafkaConfig(
    schemaRegistryUrl: URI,
    flightRawTopic: String,
    airplaneRawTopic: String,
    airportRawTopic: String,
    airlineRawTopic: String,
    cityRawTopic: String
)
