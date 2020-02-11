package it.bitrock.dvs.producer.aviationedge.kafka.models

import java.time.Instant

import it.bitrock.dvs.model.avro._
import it.bitrock.dvs.model.avro.monitoring.FlightRequestComputationStatus
import it.bitrock.dvs.producer.aviationedge.model._

object RawImplicitConversions {

  implicit private class GeographyOps(gj: GeographyJson) {
    def toGeography =
      new Geography(gj.latitude, gj.longitude, gj.altitude, gj.direction)
  }

  implicit private class SpeedOps(gj: SpeedJson) {
    def toSpeed =
      new Speed(gj.horizontal, gj.vspeed)
  }

  implicit private def toDeparture(departureJson: DepartureJson): Departure =
    new Departure(departureJson.iataCode, departureJson.icaoCode)

  implicit private def toArrival(arrivalJson: ArrivalJson): Arrival =
    new Arrival(arrivalJson.iataCode, arrivalJson.icaoCode)

  implicit private def toAirline(airlineJson: AirlineJson): Airline =
    new Airline(airlineJson.iataCode, airlineJson.icaoCode)

  implicit private class AircraftOps(gj: AircraftJson) {
    def toAircraft =
      new Aircraft(gj.regNumber, gj.icaoCode, gj.icao24, gj.iataCode)
  }

  implicit private class FlightOps(gj: FlightJson) {
    def toFlight =
      new Flight(gj.iataNumber, gj.icaoNumber, gj.number)
  }

  implicit private class SystemOps(gj: SystemJson) {
    def toSystem = new System(Instant.ofEpochSecond(gj.updated))
  }

  implicit class FlightRawStreamEventOps(mrse: FlightMessageJson) {
    def toFlightRaw: FlightRaw =
      FlightRaw(
        mrse.geography.toGeography,
        mrse.speed.toSpeed,
        mrse.departure,
        mrse.arrival,
        mrse.aircraft.toAircraft,
        mrse.airline,
        mrse.flight.toFlight,
        mrse.system.toSystem,
        mrse.status
      )
  }

  implicit class AirplaneRawStreamEventOps(mrse: AirplaneMessageJson) {
    def toAirplaneRaw: AirplaneRaw =
      AirplaneRaw(
        mrse.numberRegistration,
        mrse.productionLine,
        mrse.airplaneIataType,
        mrse.planeModel,
        mrse.modelCode,
        mrse.hexIcaoAirplane,
        mrse.codeIataPlaneLong,
        mrse.planeOwner,
        mrse.enginesType,
        mrse.planeAge,
        mrse.planeStatus
      )
  }

  implicit class AirportRawStreamEventOps(mrse: AirportMessageJson) {
    def toAirportRaw: AirportRaw =
      AirportRaw(
        mrse.airportId,
        mrse.nameAirport,
        mrse.codeIataAirport,
        mrse.latitudeAirport,
        mrse.longitudeAirport,
        mrse.nameCountry,
        mrse.codeIso2Country,
        mrse.codeIataCity,
        mrse.timezone,
        mrse.GMT
      )
  }

  implicit class AirlineRawStreamEventOps(mrse: AirlineMessageJson) {
    def toAirlineRaw: AirlineRaw =
      AirlineRaw(
        mrse.airlineId,
        mrse.nameAirline,
        mrse.codeIataAirline,
        mrse.codeIcaoAirline,
        mrse.callsign,
        mrse.statusAirline,
        mrse.sizeAirline,
        mrse.nameCountry,
        mrse.codeIso2Country
      )
  }

  implicit class CityRawStreamEventOps(mrse: CityMessageJson) {
    def toCityRaw: CityRaw =
      CityRaw(
        mrse.cityId,
        mrse.nameCity,
        mrse.codeIataCity,
        mrse.codeIso2Country,
        mrse.latitudeCity,
        mrse.longitudeCity
      )
  }

  implicit class ParserErrorStreamEventOps(mrse: ErrorMessageJson) {
    def toParserError: ParserError =
      ParserError(
        mrse.errorSource,
        mrse.errorMessage,
        mrse.failedJson,
        mrse.timestamp
      )
  }

  implicit class FlightRequestMetricsStreamEventOps(mrse: MonitoringMessageJson) {
    def toFlightRequestMetrics: FlightRequestComputationStatus =
      FlightRequestComputationStatus(
        mrse.messageReceivedOn,
        mrse.minUpdated,
        mrse.maxUpdated,
        mrse.averageUpdated,
        mrse.numErrors,
        mrse.numValid,
        mrse.numInvalid,
        mrse.total
      )
  }

}
