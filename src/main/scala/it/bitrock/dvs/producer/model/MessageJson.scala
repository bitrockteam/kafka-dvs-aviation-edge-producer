package it.bitrock.dvs.producer.model

sealed trait MessageJson extends Product

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
) extends MessageJson

final case class AirplaneMessageJson(
    numberRegistration: String,
    productionLine: String,
    airplaneIataType: String,
    planeModel: String,
    modelCode: String,
    hexIcaoAirplane: String,
    codeIataPlaneLong: String,
    planeOwner: String,
    enginesType: String,
    planeAge: String,
    planeStatus: String
) extends MessageJson

final case class AirportMessageJson(
    airportId: String,
    nameAirport: String,
    codeIataAirport: String,
    latitudeAirport: String,
    longitudeAirport: String,
    nameCountry: String,
    codeIso2Country: String,
    codeIataCity: String,
    timezone: String,
    GMT: String
) extends MessageJson

final case class AirlineMessageJson(
    airlineId: String,
    nameAirline: String,
    codeIataAirline: String,
    codeIcaoAirline: String,
    callsign: String,
    statusAirline: String,
    sizeAirline: String,
    nameCountry: String,
    codeIso2Country: String
) extends MessageJson

final case class CityMessageJson(
    cityId: String,
    nameCity: String,
    codeIataCity: String,
    codeIso2Country: String,
    latitudeCity: String,
    longitudeCity: String
) extends MessageJson

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
