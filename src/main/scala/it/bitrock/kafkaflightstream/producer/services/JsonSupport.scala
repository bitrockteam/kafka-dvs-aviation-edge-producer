package it.bitrock.kafkaflightstream.producer.services

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.bitrock.kafkaflightstream.producer.model._
import spray.json._

object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val geographyJsonFormat: RootJsonFormat[GeographyJson]             = jsonFormat4(GeographyJson.apply)
  implicit val speedJsonFormat: RootJsonFormat[SpeedJson]                     = jsonFormat2(SpeedJson.apply)
  implicit val commonCodeJsonFormat: RootJsonFormat[CommonCodeJson]           = jsonFormat2(CommonCodeJson.apply)
  implicit val aircraftJsonFormat: RootJsonFormat[AircraftJson]               = jsonFormat4(AircraftJson.apply)
  implicit val flightJsonFormat: RootJsonFormat[FlightJson]                   = jsonFormat3(FlightJson.apply)
  implicit val systemJsonFormat: RootJsonFormat[SystemJson]                   = jsonFormat2(SystemJson.apply)
  implicit val flightMessageJsonFormat: RootJsonFormat[FlightMessageJson]     = jsonFormat9(FlightMessageJson.apply)
  implicit val airplaneMessageJsonFormat: RootJsonFormat[AirplaneMessageJson] = jsonFormat11(AirplaneMessageJson.apply)
  implicit val airportMessageJsonFormat: RootJsonFormat[AirportMessageJson]   = jsonFormat10(AirportMessageJson.apply)
  implicit val airlineMessageJsonFormat: RootJsonFormat[AirlineMessageJson]   = jsonFormat9(AirlineMessageJson.apply)
  implicit val cityMessageJsonFormat: RootJsonFormat[CityMessageJson]         = jsonFormat6(CityMessageJson.apply)

  implicit val responsePayloadJsonFormat: RootJsonFormat[MessageJson] = new RootJsonFormat[MessageJson] {
    def write(obj: MessageJson): JsValue =
      JsObject((obj match {
        case c: FlightMessageJson   => c.toJson
        case c: AirplaneMessageJson => c.toJson
        case c: AirportMessageJson  => c.toJson
        case c: AirlineMessageJson  => c.toJson
        case c: CityMessageJson     => c.toJson
      }).asJsObject.fields)
    def read(json: JsValue): MessageJson =
      json.asJsObject match {
        case j: JsObject if j.getFields("flight") != Seq()     => json.convertTo[FlightMessageJson]
        case j: JsObject if j.getFields("airplaneId") != Seq() => json.convertTo[AirplaneMessageJson]
        case j: JsObject if j.getFields("airportId") != Seq()  => json.convertTo[AirportMessageJson]
        case j: JsObject if j.getFields("airlineId") != Seq()  => json.convertTo[AirlineMessageJson]
        case j: JsObject if j.getFields("cityId") != Seq()     => json.convertTo[CityMessageJson]
      }
  }

}
