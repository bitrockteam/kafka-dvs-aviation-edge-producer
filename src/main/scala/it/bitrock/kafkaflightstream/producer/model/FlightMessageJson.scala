package it.bitrock.kafkaflightstream.producer.model

final case class FlightMessageJson(
    geography: GeographyJson,
    speed: SpeedJson,
    departure: CommonCodeJson,
    arrival: CommonCodeJson,
    aircraft: AircraftJson,
    airline: CommonCodeJson,
    flight: FlightJson,
    system: SystemJson,
    status: String
)

final case class GeographyJson(
    latitude: Double,
    longitude: Double,
    altitude: Double,
    direction: Double
)

final case class SpeedJson(
    horizontal: Double,
    vertical: Double
)

final case class CommonCodeJson(
    iataCode: String,
    icaoCode: String
)

final case class AircraftJson(
    regNumber: String,
    icaoCode: String,
    icao24: String,
    iataCode: String
)

final case class FlightJson(
    iataNumber: String,
    icaoNumber: String,
    number: String
)

final case class SystemJson(
    updated: String,
    squawk: String
)
