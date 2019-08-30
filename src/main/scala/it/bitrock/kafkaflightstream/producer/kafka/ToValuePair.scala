package it.bitrock.kafkaflightstream.producer.kafka

import it.bitrock.kafkaflightstream.producer.kafka.KafkaTypes._
import it.bitrock.kafkaflightstream.producer.kafka.models.FlightRawImplicitConversions._
import it.bitrock.kafkaflightstream.producer.model.FlightMessageJson

//OK
trait ToValuePair[J, K, V] {
  def toValuePair(j: J): (K, V)
}

object ToValuePair {
  implicit val flightValuePair: ToValuePair[FlightMessageJson, Flight.Key, Flight.Value] = (j: FlightMessageJson) =>
    (j.flight.iata_number, j.toFlightRaw)

}
