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
  MessageJson
}

trait ToValuePair[J, K, V] {
  def toValuePair(j: J): (K, V)
}

object ToValuePair {
  implicit val flightValuePair: ToValuePair[MessageJson, Key, Flight.Value] = j =>
    (j.asInstanceOf[FlightMessageJson].flight.icaoNumber, j.asInstanceOf[FlightMessageJson].toFlightRaw)
  implicit val airplaneValuePair: ToValuePair[MessageJson, Key, Airplane.Value] = j =>
    (j.asInstanceOf[AirplaneMessageJson].numberRegistration, j.asInstanceOf[AirplaneMessageJson].toAirplaneRaw)
  implicit val airportValuePair: ToValuePair[MessageJson, Key, Airport.Value] = j =>
    (j.asInstanceOf[AirportMessageJson].codeIataAirport, j.asInstanceOf[AirportMessageJson].toAirportRaw)
  implicit val airlineValuePair: ToValuePair[MessageJson, Key, Airline.Value] = j =>
    (j.asInstanceOf[AirlineMessageJson].codeIcaoAirline, j.asInstanceOf[AirlineMessageJson].toAirlineRaw)
  implicit val cityValuePair: ToValuePair[MessageJson, Key, City.Value] = j =>
    (j.asInstanceOf[CityMessageJson].codeIataCity, j.asInstanceOf[CityMessageJson].toCityRaw)
  @SuppressWarnings(Array("scalafix:DisableSyntax.null"))
  implicit val parserErrorValuePair: ToValuePair[ErrorMessageJson, Key, Error.Value] = j => (null, j.toParserError)
}
