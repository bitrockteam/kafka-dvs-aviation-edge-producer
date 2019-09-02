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
    iata_code: String,
    icao_code: String
)

final case class AircraftJson(
    reg_number: String,
    icao_code: String,
    icao24: String,
    iata_code: String
)

final case class FlightJson(
    iata_number: String,
    icao_number: String,
    number: String
)

final case class SystemJson(
    updated: String,
    squawk: String
)
