package it.bitrock.dvs.producer.aviationedge

import java.time.Instant

import it.bitrock.dvs.model.avro._
import it.bitrock.dvs.producer.aviationedge.model._

trait TestValues {

  final val IcaoNumber = "SWR6U"

  final val FlightMessage = FlightMessageJson(
    GeographyJson(49.2655, -1.9623, 9753.6, 282.76),
    SpeedJson(805.14, 0),
    DepartureJson(Some("ZRH"), "LSZH"),
    ArrivalJson(Some("ORD"), "KORD"),
    AircraftJson("HBJHA", "A333", "", "A333"),
    AirlineJson(Some("LX"), "SWR"),
    FlightJson(Some("LX6U"), IcaoNumber, "6U"),
    SystemJson(1567415880),
    "en-route"
  )

  final val ExpectedFlightRaw = FlightRaw(
    Geography(49.2655, -1.9623, 9753.6, 282.76),
    Speed(805.14, 0),
    Departure("ZRH", "LSZH"),
    Arrival("ORD", "KORD"),
    Aircraft("HBJHA", "A333", "", "A333"),
    Airline("LX", "SWR"),
    Flight("LX6U", IcaoNumber, "6U"),
    System(Instant.ofEpochSecond(1567415880)),
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
  final val InvalidDepartureFlightMessage = FlightMessage.copy(departure = DepartureJson(Some(""), ""))
  final val InvalidArrivalFlightMessage   = FlightMessage.copy(arrival = ArrivalJson(Some(""), ""))

}
