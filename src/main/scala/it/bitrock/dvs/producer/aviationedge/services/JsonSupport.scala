package it.bitrock.dvs.producer.aviationedge.services

import java.time.Instant

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.bitrock.dvs.producer.aviationedge.model._
import spray.json._

import scala.util.{Failure, Success, Try}

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
  implicit val flightStatesJsonFormat: RootJsonFormat[FlightStatesJson]       = jsonFormat2(FlightStatesJson.apply)

  implicit val responsePayloadJsonFormat: RootJsonFormat[List[Either[ErrorMessageJson, MessageJson]]] =
    new RootJsonFormat[List[Either[ErrorMessageJson, MessageJson]]] {
      def write(obj: List[Either[ErrorMessageJson, MessageJson]]): JsValue = JsNull
      def read(json: JsValue): List[Either[ErrorMessageJson, MessageJson]] =
        json match {
          case _: JsObject =>
            Try(json.convertTo[FlightStatesJson]) match {
              case Failure(ex) => List(Left(ErrorMessageJson("", ex.getMessage, json.compactPrint, Instant.now)))
              case Success(flightStates) =>
                flightStates.states.map { state =>
                  Try(
                    FlightStateJson(
                      state(1).convertTo[String],
                      state(3).convertTo[Long],
                      state(5).convertTo[Double],
                      state(6).convertTo[Double],
                      state(9).convertTo[Double],
                      state(10).convertTo[Double],
                      state(13).convertTo[Double]
                    )
                  ).toEither.left.map(ex => ErrorMessageJson("", ex.getMessage, json.compactPrint, Instant.now))
                }
            }
          case JsArray(elements) =>
            elements
              .map(json =>
                Try(
                  json.asJsObject match {
                    case j: JsObject if j.getFields("flight") != Seq()     => json.convertTo[FlightMessageJson]
                    case j: JsObject if j.getFields("airplaneId") != Seq() => json.convertTo[AirplaneMessageJson]
                    case j: JsObject if j.getFields("airportId") != Seq()  => json.convertTo[AirportMessageJson]
                    case j: JsObject if j.getFields("airlineId") != Seq()  => json.convertTo[AirlineMessageJson]
                    case j: JsObject if j.getFields("cityId") != Seq()     => json.convertTo[CityMessageJson]
                  }
                ).toEither.left.map(ex => ErrorMessageJson("", ex.getMessage, json.compactPrint, Instant.now))
              )
              .toList
          case _ => List(Left(ErrorMessageJson("", "", json.compactPrint, Instant.now)))
        }
    }
}
