package it.bitrock.kafkaflightstream.producer.kafka

import it.bitrock.kafkaflightstream.model.FlightRaw

object KafkaTypes {
  object Flight {
    type Key   = String
    type Value = FlightRaw
  }
}
