package it.bitrock.dvs.producer.aviationedge.kafka

import it.bitrock.dvs.model.avro.{AirlineRaw, AirplaneRaw, AirportRaw, CityRaw, FlightRaw}

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
