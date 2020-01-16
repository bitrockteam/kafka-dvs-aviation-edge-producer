package it.bitrock.dvs.producer

import java.time.Instant

import it.bitrock.dvs.producer.model.{
  AircraftJson,
  AirlineMessageJson,
  CommonCodeJson,
  FlightJson,
  FlightMessageJson,
  GeographyJson,
  SpeedJson,
  SystemJson
}
import it.bitrock.dvs.model.avro.{Aircraft, CommonCode, Flight, FlightRaw, Geography, Speed, System}

trait TestValues {

  final val IcaoNumber = "SWR6U"

  final val FlightMessage = FlightMessageJson(
    GeographyJson(49.2655, -1.9623, 9753.6, 282.76),
    SpeedJson(805.14, 0),
    CommonCodeJson(Some("ZRH"), "LSZH"),
    CommonCodeJson(Some("ORD"), "KORD"),
    AircraftJson("HBJHA", "A333", "", "A333"),
    CommonCodeJson(Some("LX"), "SWR"),
    FlightJson(Some("LX6U"), IcaoNumber, "6U"),
    SystemJson(1567415880),
    "en-route"
  )

  final val ExpectedFlightRaw = FlightRaw(
    Geography(49.2655, -1.9623, 9753.6, 282.76),
    Speed(805.14, 0),
    CommonCode("ZRH", "LSZH"),
    CommonCode("ORD", "KORD"),
    Aircraft("HBJHA", "A333", "", "A333"),
    CommonCode("LX", "SWR"),
    Flight("LX6U", IcaoNumber, "6U"),
    System(Instant.ofEpochMilli(1567415880)),
    "en-route"
  )

  final val ValidAirlineMessage           = AirlineMessageJson(0, "", "", "", "", "active", 0, "", "")
  final val InvalidAirlineMessage         = AirlineMessageJson(0, "", "", "", "", "invalid status", 0, "", "")
  final val StartedFlightMessage          = FlightMessage.copy(status = "started")
  final val EnRouteFlightMessage          = FlightMessage.copy(status = "en-route")
  final val LandedFlightMessage           = FlightMessage.copy(status = "landed")
  final val UnknownFlightMessage          = FlightMessage.copy(status = "unknown")
  final val CrashedFlightMessage          = FlightMessage.copy(status = "crashed")
  final val InvalidSpeedFlightMessage     = FlightMessage.copy(speed = SpeedJson(1300.00, 0.0))
  final val InvalidDepartureFlightMessage = FlightMessage.copy(departure = CommonCodeJson(Some(""), ""))
  final val InvalidArrivalFlightMessage   = FlightMessage.copy(arrival = CommonCodeJson(Some(""), ""))

}
