package it.bitrock.dvs.producer.aviationedge.kafka

import KafkaTypes._
import it.bitrock.dvs.producer.aviationedge.kafka.models.RawImplicitConversions._
import it.bitrock.dvs.producer.aviationedge.model.{
  AirlineMessageJson,
  AirplaneMessageJson,
  AirportMessageJson,
  CityMessageJson,
  ErrorMessageJson,
  FlightMessageJson,
  FlightStateJson,
  MonitoringMessageJson
}

trait ToValuePair[J, K, V] {
  def toValuePair(j: J): (K, V)
}

@SuppressWarnings(Array("scalafix:DisableSyntax.null"))
object ToValuePair {
  implicit def messageJsonValuePair[A]: ToValuePair[A, Key, Flight.Value] =
    j => (j.asInstanceOf[FlightMessageJson].flight.icaoNumber, j.asInstanceOf[FlightMessageJson].toFlightRaw)
  implicit val flightValuePair: ToValuePair[FlightMessageJson, Key, Flight.Value] = j => (j.flight.icaoNumber, j.toFlightRaw)
  implicit val airplaneValuePair: ToValuePair[AirplaneMessageJson, Key, Airplane.Value] = j =>
    (j.numberRegistration, j.toAirplaneRaw)
  implicit val airportValuePair: ToValuePair[AirportMessageJson, Key, Airport.Value] = j => (j.codeIataAirport, j.toAirportRaw)
  implicit val airlineValuePair: ToValuePair[AirlineMessageJson, Key, Airline.Value] = j => (j.codeIcaoAirline, j.toAirlineRaw)
  implicit val cityValuePair: ToValuePair[CityMessageJson, Key, City.Value]          = j => (j.codeIataCity, j.toCityRaw)
  implicit val parserErrorValuePair: ToValuePair[ErrorMessageJson, Key, Error.Value] = j => (null, j.toParserError)
  implicit val monitoringErrorValuePair: ToValuePair[MonitoringMessageJson, Key, Monitoring.Value] = j =>
    (null, j.toFlightRequestMetrics)
  implicit val flightStateValuePair: ToValuePair[FlightStateJson, Key, FlightState.Value] = j => (j.callsign, j.toFlightStateRaw)

}
