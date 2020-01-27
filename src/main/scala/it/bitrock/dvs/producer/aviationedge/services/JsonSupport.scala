package it.bitrock.dvs.producer.aviationedge.services

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.bitrock.dvs.producer.aviationedge.model._
import spray.json._

import scala.util.Try

object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val departureJsonFormat: RootJsonFormat[DepartureJson]             = jsonFormat2(DepartureJson.apply)
  implicit val arrivalJsonFormat: RootJsonFormat[ArrivalJson]                 = jsonFormat2(ArrivalJson.apply)
  implicit val airportJsonFormat: RootJsonFormat[AirlineJson]                 = jsonFormat2(AirlineJson.apply)
  implicit val geographyJsonFormat: RootJsonFormat[GeographyJson]             = jsonFormat4(GeographyJson.apply)
  implicit val speedJsonFormat: RootJsonFormat[SpeedJson]                     = jsonFormat2(SpeedJson.apply)
  implicit val aircraftJsonFormat: RootJsonFormat[AircraftJson]               = jsonFormat4(AircraftJson.apply)
  implicit val flightJsonFormat: RootJsonFormat[FlightJson]                   = jsonFormat3(FlightJson.apply)
  implicit val systemJsonFormat: RootJsonFormat[SystemJson]                   = jsonFormat1(SystemJson.apply)
  implicit val flightMessageJsonFormat: RootJsonFormat[FlightMessageJson]     = jsonFormat9(FlightMessageJson.apply)
  implicit val airplaneMessageJsonFormat: RootJsonFormat[AirplaneMessageJson] = jsonFormat11(AirplaneMessageJson.apply)
  implicit val airportMessageJsonFormat: RootJsonFormat[AirportMessageJson]   = jsonFormat10(AirportMessageJson.apply)
  implicit val airlineMessageJsonFormat: RootJsonFormat[AirlineMessageJson]   = jsonFormat9(AirlineMessageJson.apply)
  implicit val cityMessageJsonFormat: RootJsonFormat[CityMessageJson]         = jsonFormat6(CityMessageJson.apply)

  implicit val responsePayloadJsonFormat: RootJsonFormat[Either[ErrorMessageJson, MessageJson]] =
    new RootJsonFormat[Either[ErrorMessageJson, MessageJson]] {
      def write(obj: Either[ErrorMessageJson, MessageJson]): JsValue = JsNull
      def read(json: JsValue): Either[ErrorMessageJson, MessageJson] =
        Try(
          json.asJsObject match {
            case j: JsObject if j.getFields("flight") != Seq()     => json.convertTo[FlightMessageJson]
            case j: JsObject if j.getFields("airplaneId") != Seq() => json.convertTo[AirplaneMessageJson]
            case j: JsObject if j.getFields("airportId") != Seq()  => json.convertTo[AirportMessageJson]
            case j: JsObject if j.getFields("airlineId") != Seq()  => json.convertTo[AirlineMessageJson]
            case j: JsObject if j.getFields("cityId") != Seq()     => json.convertTo[CityMessageJson]
          }
        ).toEither.left.map { ex =>
          ErrorMessageJson(ex.getMessage, json.compactPrint)
        }
    }

}
