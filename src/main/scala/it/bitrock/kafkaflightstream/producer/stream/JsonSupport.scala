package it.bitrock.kafkaflightstream.producer.stream

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.bitrock.kafkaflightstream.producer.model._
import spray.json._

object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val geographyJsonFormat: RootJsonFormat[GeographyJson]         = jsonFormat4(GeographyJson.apply)
  implicit val speedJsonFormat: RootJsonFormat[SpeedJson]                 = jsonFormat2(SpeedJson.apply)
  implicit val commonCodeJsonFormat: RootJsonFormat[CommonCodeJson]       = jsonFormat2(CommonCodeJson.apply)
  implicit val aircraftJsonFormat: RootJsonFormat[AircraftJson]           = jsonFormat4(AircraftJson.apply)
  implicit val flightJsonFormat: RootJsonFormat[FlightJson]               = jsonFormat3(FlightJson.apply)
  implicit val systemJsonFormat: RootJsonFormat[SystemJson]               = jsonFormat2(SystemJson.apply)
  implicit val flightMessageJsonFormat: RootJsonFormat[FlightMessageJson] = jsonFormat9(FlightMessageJson.apply)

}
