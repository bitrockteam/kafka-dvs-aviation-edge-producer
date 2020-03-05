package it.bitrock.dvs.producer.aviationedge.kafka

import it.bitrock.dvs.model.avro._
import it.bitrock.dvs.model.avro.monitoring.FlightRequestComputationStatus

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
  object Error {
    type Value = ParserError
  }
  object Monitoring {
    type Value = FlightRequestComputationStatus
  }
  object FlightState {
    type Value = FlightStateRaw
  }
}
