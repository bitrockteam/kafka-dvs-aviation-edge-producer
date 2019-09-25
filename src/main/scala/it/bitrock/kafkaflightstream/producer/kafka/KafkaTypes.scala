package it.bitrock.kafkaflightstream.producer.kafka

import it.bitrock.kafkaflightstream.model.{AirlineRaw, AirplaneRaw, AirportRaw, CityRaw, FlightRaw}

object KafkaTypes {

  type Key = String

  object Flight {
    type Value = FlightRaw
  }
  object Airplane {
    type Value = AirplaneRaw
  }
  object Airport {
    type Value = AirportRaw
  }
  object Airline {
    type Value = AirlineRaw
  }
  object City {
    type Value = CityRaw
  }

}
