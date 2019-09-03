package it.bitrock.kafkaflightstream.producer.kafka.models

import it.bitrock.kafkaflightstream.model._
import it.bitrock.kafkaflightstream.producer.model._

object FlightRawImplicitConversions {

  implicit class GeographyOps(gj: GeographyJson) {
    def toGeography =
      new Geography(gj.latitude, gj.longitude, gj.altitude, gj.direction)
  }

  implicit class SpeedOps(gj: SpeedJson) {
    def toSpeed =
      new Speed(gj.horizontal, gj.vertical)
  }

  implicit class CommonOps(gj: CommonCodeJson) {
    def toCommon =
      new CommonCode(gj.iataCode, gj.icaoCode)
  }

  implicit class AircraftOps(gj: AircraftJson) {
    def toAircraft =
      new Aircraft(gj.regNumber, gj.icaoCode, gj.icao24, gj.iataCode)
  }

  implicit class FlightOps(gj: FlightJson) {
    def toFlight =
      new Flight(gj.iataNumber, gj.icaoNumber, gj.number)
  }

  implicit class SystemOps(gj: SystemJson) {
    def toSystem =
      new System(gj.updated, gj.squawk)
  }

  implicit class FlightRawStreamEventOps(mrse: FlightMessageJson) {
    def toFlightRaw: FlightRaw =
      FlightRaw(
        mrse.geography.toGeography,
        mrse.speed.toSpeed,
        mrse.departure.toCommon,
        mrse.arrival.toCommon,
        mrse.aircraft.toAircraft,
        mrse.airline.toCommon,
        mrse.flight.toFlight,
        mrse.system.toSystem,
        mrse.status
      )
  }
}
