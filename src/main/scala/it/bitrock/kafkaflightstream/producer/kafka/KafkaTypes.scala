package it.bitrock.kafkaflightstream.producer.kafka

import it.bitrock.kafkaflightstream.model.{AirlineRaw, AirplaneRaw, AirportRaw, CityRaw, FlightRaw}

object KafkaTypes {
  object Flight {
    type Key   = String
    type Value = FlightRaw
  }
  object Airplane {
    type Key   = String
    type Value = AirplaneRaw
  }
  object Airport {
    type Key   = String
    type Value = AirportRaw
  }
  object Airline {
    type Key   = String
    type Value = AirlineRaw
  }
  object City {
    type Key   = String
    type Value = CityRaw
  }
}
