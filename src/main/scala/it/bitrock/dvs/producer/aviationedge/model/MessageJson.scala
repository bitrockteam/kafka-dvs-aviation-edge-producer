package it.bitrock.dvs.producer.aviationedge.model

sealed trait MessageJson extends Product

final case class FlightMessageJson(
    geography: GeographyJson,
    speed: SpeedJson,
    departure: DepartureJson,
    arrival: ArrivalJson,
    aircraft: AircraftJson,
    airline: AirlineJson,
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
    airportId: Long,
    nameAirport: String,
    codeIataAirport: String,
    latitudeAirport: Double,
    longitudeAirport: Double,
    nameCountry: String,
    codeIso2Country: String,
    codeIataCity: String,
    timezone: String,
    GMT: String
) extends MessageJson

final case class AirlineMessageJson(
    airlineId: Long,
    nameAirline: String,
    codeIataAirline: String,
    codeIcaoAirline: String,
    callsign: String,
    statusAirline: String,
    sizeAirline: Long,
    nameCountry: String,
    codeIso2Country: String
) extends MessageJson

final case class CityMessageJson(
    cityId: Long,
    nameCity: String,
    codeIataCity: String,
    codeIso2Country: String,
    latitudeCity: Double,
    longitudeCity: Double
) extends MessageJson

final case class GeographyJson(
    latitude: Double,
    longitude: Double,
    altitude: Double,
    direction: Double
)

final case class SpeedJson(
    horizontal: Double,
    vspeed: Double
)

final case class DepartureJson(
    iataCode: Option[String],
    icaoCode: String
)

final case class ArrivalJson(
    iataCode: Option[String],
    icaoCode: String
)

final case class AirlineJson(
    iataCode: Option[String],
    icaoCode: String
)

final case class AircraftJson(
    regNumber: String,
    icaoCode: String,
    icao24: String,
    iataCode: String
)

final case class FlightJson(
    iataNumber: Option[String],
    icaoNumber: String,
    number: String
)

final case class SystemJson(
    updated: Long
)
