package it.bitrock.dvs.producer.aviationedge.config

import java.net.URI
import java.time.DayOfWeek

import akka.http.scaladsl.model.Uri
import pureconfig.ConfigSource
import pureconfig.generic.auto._

final case class ApiProviderConfig(aviationEdge: AviationConfig, openSky: OpenSkyConfig)

final case class AviationConfig(
    host: URI,
    key: String,
    apiTimeout: Int,
    flightSpeedLimit: Int,
    tickSource: TickSourceConfig,
    flightStream: ApiProviderStreamConfig,
    airplaneStream: ApiProviderStreamConfig,
    airportStream: ApiProviderStreamConfig,
    airlineStream: ApiProviderStreamConfig,
    cityStream: ApiProviderStreamConfig
) {
  def getAviationUri(path: String): String = {
    val query = Uri.Query("key" -> key)
    Uri(host.resolve(path).toString)
      .withQuery(query)
      .toString
  }
}

object AviationConfig {
  def load: AviationConfig = ConfigSource.default.at("aviation").loadOrThrow[AviationConfig]
}

final case class OpenSkyConfig(
    host: URI,
    apiTimeout: Int,
    tickSource: TickSourceConfig,
    flightStateStream: ApiProviderStreamConfig
) {
  def getOpenSkyUri(path: String): String =
    host.resolve(path).toString
}

object OpenSkyConfig {
  def load: OpenSkyConfig = ConfigSource.default.at("opensky").loadOrThrow[OpenSkyConfig]
}

final case class ApiProviderStreamConfig(
    path: String,
    pollingStart: Int,
    pollingInterval: Int
)

final case class TickSourceConfig(pollLowerHourLimit: Int, pollUpperHourLimit: Int, pollExcludedDays: List[DayOfWeek])
