package it.bitrock.dvs.producer.aviationedge.services

import java.time.Instant

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers._fromStringUnmarshallerFromByteStringUnmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller
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

  implicit val flightStatesJsonReader: JsonReader[FlightStateJson] = new JsonReader[FlightStateJson] {
    override def read(json: JsValue): FlightStateJson =
      json match {
        case jsArray: JsArray =>
          Try(
            FlightStateJson(
              callsign = jsArray.elements(1).convertTo[String].trim.toUpperCase,
              time_position = jsArray.elements(3).convertTo[Long],
              longitude = jsArray.elements(5).convertTo[Double],
              latitude = jsArray.elements(6).convertTo[Double],
              velocity = jsArray.elements(9).convertTo[Double],
              true_track = jsArray.elements(10).convertTo[Double],
              geo_altitude = jsArray.elements(13).convertTo[Double]
            )
          ).getOrElse(deserializationError("Invalid JsArray for FlightStateJson, got " + jsArray))
        case jsValue => deserializationError("Expected FlightStateJson as JsArray, but got " + jsValue)
      }
  }

  def aviationEdgePayloadJsonReader[A: JsonReader]: RootJsonReader[List[Either[ErrorMessageJson, A]]] = {
    case jsArray: JsArray => jsArrayToResponsePayload[A](jsArray)
    case json             => List(Left(ErrorMessageJson("", "", json.compactPrint, Instant.now)))
  }

  def openSkyResponsePayloadJsonFormat[A: JsonReader]: RootJsonReader[List[Either[ErrorMessageJson, A]]] = {
    case jsObject: JsObject => jsObjectToResponsePayload[A](jsObject)
    case json               => List(Left(ErrorMessageJson("", "", json.compactPrint, Instant.now)))
  }

  implicit def unmarshallerFrom[A](rf: RootJsonReader[A]): Unmarshaller[String, A] =
    _fromStringUnmarshallerFromByteStringUnmarshaller(sprayJsonByteStringUnmarshaller(rf))

  private def jsObjectToResponsePayload[A: JsonReader](json: JsObject): List[Either[ErrorMessageJson, A]] =
    Try(json.convertTo[FlightStatesJson]) match {
      case Failure(ex) => List(Left(ErrorMessageJson("", ex.getMessage, json.compactPrint, Instant.now)))
      case Success(flightStates) =>
        flightStates.states.map { state =>
          Try(
            JsArray(state.toVector).convertTo[A]
          ).toEither.left.map(ex => ErrorMessageJson("", ex.getMessage, json.compactPrint, Instant.now))
        }
    }

  private def jsArrayToResponsePayload[A: JsonReader](json: JsArray): List[Either[ErrorMessageJson, A]] =
    json
      .asInstanceOf[JsArray]
      .elements
      .map(json =>
        Try(json.convertTo[A]).toEither.left.map(ex => ErrorMessageJson("", ex.getMessage, json.compactPrint, Instant.now))
      )
      .toList
}
