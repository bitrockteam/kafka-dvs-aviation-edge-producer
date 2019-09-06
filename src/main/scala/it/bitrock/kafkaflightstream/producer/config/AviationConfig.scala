package it.bitrock.kafkaflightstream.producer.config

import pureconfig.generic.auto._
import java.net.URI

import akka.http.scaladsl.model.Uri
import it.bitrock.kafkaflightstream.producer.model.{AirlineStream, AirplaneStream, AirportStream, AviationStream, CityStream, FlightStream}

final case class AviationConfig(
    host: URI,
    key: String,
    flightStream: AviationStreamConfig,
    airplaneStream: AviationStreamConfig,
    airportStream: AviationStreamConfig,
    airlineStream: AviationStreamConfig,
    cityStream: AviationStreamConfig
) {
  def getAviationUri(obj: AviationStream): String = {
    val query = Uri.Query("key" -> key)
    val path = obj match {
      case FlightStream   => flightStream.path
      case AirplaneStream => airplaneStream.path
      case AirportStream  => airportStream.path
      case AirlineStream  => airlineStream.path
      case CityStream     => cityStream.path
    }
    Uri(host.resolve(path).toString)
      .withQuery(query)
      .toString
  }
}

final case class AviationStreamConfig(
    path: String,
    pollingInterval: Int
)

object AviationConfig {

  def load: AviationConfig = pureconfig.loadConfigOrThrow[AviationConfig]("aviation")

}
