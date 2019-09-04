package it.bitrock.kafkaflightstream.producer.config

import pureconfig.generic.auto._

import java.net.URI

import akka.http.scaladsl.model.Uri

final case class AviationConfig(
    flightStream: AviationStreamConfig
)

object AviationConfig {

  def load: AviationConfig = pureconfig.loadConfigOrThrow[AviationConfig]("aviation")

}

final case class AviationStreamConfig(
    host: URI,
    path: String,
    key: String,
    pollingInterval: Int
) {
  def getAviationUri: String = {
    val query = Uri.Query("key" -> key)
    Uri(host.resolve(path).toString)
      .withQuery(query)
      .toString
  }
}
