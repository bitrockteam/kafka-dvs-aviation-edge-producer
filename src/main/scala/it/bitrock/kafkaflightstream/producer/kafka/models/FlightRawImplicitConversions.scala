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
      new CommonCode(gj.iata_code, gj.icao_code)
  }

  implicit class AircraftOps(gj: AircraftJson) {
    def toAircraft =
      new Aircraft(gj.reg_number, gj.icao_code, gj.icao24, gj.iata_code)
  }

  implicit class FlightOps(gj: FlightJson) {
    def toFlight =
      new Flight(gj.iata_number, gj.icao_number, gj.number)
  }

  implicit class SystemOps(gj: SystemJson) {
    def toSystem =
      new System(gj.updated, gj.squawk)
  }

  implicit class FlightRowStreamEventOps(mrse: FlightMessageJson) {
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
