package it.bitrock.dvs.producer.aviationedge.config

import java.net.URI
import java.time.DayOfWeek

import akka.http.scaladsl.model.Uri
import pureconfig.generic.auto._

final case class AviationConfig(
    host: URI,
    key: String,
    apiTimeout: Int,
    flightSpeedLimit: Int,
    tickSource: TickSourceConfig,
    flightStream: AviationStreamConfig,
    airplaneStream: AviationStreamConfig,
    airportStream: AviationStreamConfig,
    airlineStream: AviationStreamConfig,
    cityStream: AviationStreamConfig
) {
  def getAviationUri(path: String): String = {
    val query = Uri.Query("key" -> key)
    Uri(host.resolve(path).toString)
      .withQuery(query)
      .toString
  }
}

final case class AviationStreamConfig(
    path: String,
    pollingStart: Int,
    pollingInterval: Int
)

final case class TickSourceConfig(pollLowerHourLimit: Int, pollUpperHourLimit: Int, pollExcludedDays: List[DayOfWeek])

object AviationConfig {
  def load: AviationConfig = pureconfig.loadConfigOrThrow[AviationConfig]("aviation")
}
