package it.bitrock.dvs.producer.aviationedge

import java.time.Instant

import it.bitrock.dvs.model.avro._
import it.bitrock.dvs.model.avro.monitoring.FlightRequestComputationStatus
import it.bitrock.dvs.producer.aviationedge.model._

trait TestValues {
  final val IcaoNumber    = "SWR6U"
  final val Timestamp     = Instant.now()
  final val Content       = "a content"
  final val Path          = "a path"
  final val ErrorResponse = "an error"
  final val MinUpdated    = 1567414880L
  final val Updated       = 1567415880L
  final val MaxUpdated    = 1567416880L

  final val FlightMessage = FlightMessageJson(
    GeographyJson(49.2655, -1.9623, 9753.6, 282.76),
    SpeedJson(805.14, 0),
    DepartureJson("ZRH", Some("LSZH")),
    ArrivalJson("ORD", Some("KORD")),
    AircraftJson("HBJHA", "A333", "", "A333"),
    AirlineJson(Some("LX"), "SWR"),
    FlightJson("LX6U", IcaoNumber, "6U"),
    SystemJson(Updated),
    "en-route"
  )
  final val ExpectedFlightRaw = FlightRaw(
    Geography(49.2655, -1.9623, 9753.6, 282.76),
    Speed(805.14, 0),
    Departure("ZRH", Some("LSZH")),
    Arrival("ORD", Some("KORD")),
    Aircraft("HBJHA", "A333", "", "A333"),
    Airline(Some("LX"), "SWR"),
    Flight("LX6U", IcaoNumber, "6U"),
    System(Instant.ofEpochSecond(Updated)),
    "en-route"
  )

  final val ErrorMessage        = ErrorMessageJson(Path, "a message", "a failed json", Timestamp)
  final val ExpectedParserError = ParserError(Path, "a message", "a failed json", Timestamp)

  final val MonitoringMessage = MonitoringMessageJson(Timestamp, Some(Timestamp), Some(Timestamp), Some(Timestamp), 0, 0, 0, 0)
  final val ExpectedMonitoringMessage =
    FlightRequestComputationStatus(Timestamp, Some(Timestamp), Some(Timestamp), Some(Timestamp), 0, 0, 0, 0)

  final val ValidAirlineMessage           = AirlineMessageJson(0, "", "", "", "", "active", 0, "", "")
  final val InvalidAirlineMessage         = AirlineMessageJson(0, "", "", "", "", "invalid status", 0, "", "")
  final val StartedFlightMessage          = FlightMessage.copy(status = "started")
  final val EnRouteFlightMessage          = FlightMessage.copy(status = "en-route")
  final val LandedFlightMessage           = FlightMessage.copy(status = "landed")
  final val UnknownFlightMessage          = FlightMessage.copy(status = "unknown")
  final val CrashedFlightMessage          = FlightMessage.copy(status = "crashed")
  final val InvalidSpeedFlightMessage     = FlightMessage.copy(speed = SpeedJson(1300.00, 0.0))
  final val InvalidDepartureFlightMessage = FlightMessage.copy(departure = DepartureJson("", Some("")))
  final val InvalidArrivalFlightMessage   = FlightMessage.copy(arrival = ArrivalJson("", Some("")))
  final val MinUpdatedFlightMessage       = FlightMessage.copy(system = SystemJson(MinUpdated))
  final val MaxUpdatedFlightMessage       = FlightMessage.copy(system = SystemJson(MaxUpdated))

  final val IncorrectJsonAirline =
    """
      |[
      |  {
      |    "ageFleet": 10.9,
      |    "airlineId":1,
      |    "callsign": "AMERICAN",
      |    "codeHub": "DFW",
      |    "codeIataAirline": "AA",
      |    "codeIcaoAirline": "AAL",
      |    "codeIso2Country": "US",
      |    "founding": 1934,
      |    "iataPrefixAccounting": "1",
      |    "nameAirline": null,
      |    "nameCountry": "United States",
      |    "sizeAirline": 963,
      |    "statusAirline": "active",
      |    "type": "scheduled"
      |  }
      |]
      |""".stripMargin

  final val FlightStateMessage = FlightStateJson(
    IcaoNumber,
    Updated,
    -1.9623,
    49.2655,
    805.14,
    282.76,
    9753.6
  )
  final val ExpectedFlightStateRaw = FlightStateRaw(
    IcaoNumber,
    Instant.ofEpochSecond(Updated),
    Geography(49.2655, -1.9623, 9753.6, 282.76),
    2898.504
  )
}
