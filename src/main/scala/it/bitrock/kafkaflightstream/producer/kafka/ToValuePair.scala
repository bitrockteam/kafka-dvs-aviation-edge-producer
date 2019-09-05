package it.bitrock.kafkaflightstream.producer.kafka

import it.bitrock.kafkaflightstream.producer.kafka.KafkaTypes._
import it.bitrock.kafkaflightstream.producer.kafka.models.RawImplicitConversions._
import it.bitrock.kafkaflightstream.producer.model.{AirlineMessageJson, AirportMessageJson, CityMessageJson, FlightMessageJson, MessageJson}

trait ToValuePair[J, K, V] {
  def toValuePair(j: J): (K, V)
}

object ToValuePair {
  implicit val flightValuePair: ToValuePair[MessageJson, Flight.Key, Flight.Value] = (j: MessageJson) =>
    (j.asInstanceOf[FlightMessageJson].flight.icaoNumber, j.asInstanceOf[FlightMessageJson].toFlightRaw)
  implicit val airportValuePair: ToValuePair[MessageJson, Airport.Key, Airport.Value] = (j: MessageJson) =>
    (j.asInstanceOf[AirportMessageJson].codeIataAirport, j.asInstanceOf[AirportMessageJson].toAirportRaw)
  implicit val airlineValuePair: ToValuePair[MessageJson, Airline.Key, Airline.Value] = (j: MessageJson) =>
    (j.asInstanceOf[AirlineMessageJson].codeIcaoAirline, j.asInstanceOf[AirlineMessageJson].toAirlineRaw)
  implicit val cityValuePair: ToValuePair[MessageJson, City.Key, City.Value] = (j: MessageJson) =>
    (j.asInstanceOf[CityMessageJson].codeIataCity, j.asInstanceOf[CityMessageJson].toCityRaw)
}
