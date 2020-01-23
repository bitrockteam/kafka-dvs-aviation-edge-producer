package it.bitrock.dvs.producer.aviationedge.model

sealed trait AviationStream
case object FlightStream   extends AviationStream
case object AirplaneStream extends AviationStream
case object AirportStream  extends AviationStream
case object AirlineStream  extends AviationStream
case object CityStream     extends AviationStream

case class Tick()
