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
  final val MinUpdated    = 1567414880
  final val Updated       = 1567415880
  final val MaxUpdated    = 1567416880

  final val FlightMessage = FlightMessageJson(
    GeographyJson(49.2655, -1.9623, 9753.6, 282.76),
    SpeedJson(805.14, 0),
    DepartureJson("ZRH", "LSZH"),
    ArrivalJson("ORD", "KORD"),
    AircraftJson("HBJHA", "A333", "", "A333"),
    AirlineJson("LX", "SWR"),
    FlightJson("LX6U", IcaoNumber, "6U"),
    SystemJson(Updated),
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
    System(Instant.ofEpochSecond(Updated)),
    "en-route"
  )

  final val ErrorMessage        = ErrorMessageJson(Path, "a message", "a failed json", Timestamp)
  final val ExpectedParserError = ParserError(Path, "a message", "a failed json", Timestamp)

  final val MonitoringMessage         = MonitoringMessageJson(Timestamp, Timestamp, Timestamp, 0, 0, 0)
  final val ExpectedMonitoringMessage = FlightRequestComputationStatus(Timestamp, Timestamp, Timestamp, 0, 0, 0)

  final val ValidAirlineMessage           = AirlineMessageJson(0, "", "", "", "", "active", 0, "", "")
  final val InvalidAirlineMessage         = AirlineMessageJson(0, "", "", "", "", "invalid status", 0, "", "")
  final val StartedFlightMessage          = FlightMessage.copy(status = "started")
  final val EnRouteFlightMessage          = FlightMessage.copy(status = "en-route")
  final val LandedFlightMessage           = FlightMessage.copy(status = "landed")
  final val UnknownFlightMessage          = FlightMessage.copy(status = "unknown")
  final val CrashedFlightMessage          = FlightMessage.copy(status = "crashed")
  final val InvalidSpeedFlightMessage     = FlightMessage.copy(speed = SpeedJson(1300.00, 0.0))
  final val InvalidDepartureFlightMessage = FlightMessage.copy(departure = DepartureJson("", ""))
  final val InvalidArrivalFlightMessage   = FlightMessage.copy(arrival = ArrivalJson("", ""))
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

}
