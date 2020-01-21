package it.bitrock.dvs.producer.kafka

import KafkaTypes._
import it.bitrock.dvs.producer.kafka.models.RawImplicitConversions._
import it.bitrock.dvs.producer.model.{
  AirlineMessageJson,
  AirplaneMessageJson,
  AirportMessageJson,
  CityMessageJson,
  FlightMessageJson,
  MessageJson
}

trait ToValuePair[J, K, V] {
  def toValuePair(j: J): (K, V)
}

object ToValuePair {
  implicit val flightValuePair: ToValuePair[MessageJson, Key, Flight.Value] = (j: MessageJson) =>
    (j.asInstanceOf[FlightMessageJson].flight.icaoNumber, j.asInstanceOf[FlightMessageJson].toFlightRaw)
  implicit val airplaneValuePair: ToValuePair[MessageJson, Key, Airplane.Value] = (j: MessageJson) =>
    (j.asInstanceOf[AirplaneMessageJson].numberRegistration, j.asInstanceOf[AirplaneMessageJson].toAirplaneRaw)
  implicit val airportValuePair: ToValuePair[MessageJson, Key, Airport.Value] = (j: MessageJson) =>
    (j.asInstanceOf[AirportMessageJson].codeIataAirport, j.asInstanceOf[AirportMessageJson].toAirportRaw)
  implicit val airlineValuePair: ToValuePair[MessageJson, Key, Airline.Value] = (j: MessageJson) =>
    (j.asInstanceOf[AirlineMessageJson].codeIcaoAirline, j.asInstanceOf[AirlineMessageJson].toAirlineRaw)
  implicit val cityValuePair: ToValuePair[MessageJson, Key, City.Value] = (j: MessageJson) =>
    (j.asInstanceOf[CityMessageJson].codeIataCity, j.asInstanceOf[CityMessageJson].toCityRaw)
}
