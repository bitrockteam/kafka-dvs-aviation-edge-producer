package it.bitrock.dvs.producer.aviationedge.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.testkit.TestKit
import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.testcommons.Suite
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global

class AviationFlowSpec
    extends TestKit(ActorSystem("AviationFlowSpec"))
    with Suite
    with AnyWordSpecLike
    with TestValues
    with EitherValues
    with ScalaFutures {

  private val aviationFlow = new AviationFlow()

  "extract method" should {
    "return the body for any correct response" in {
      val response     = HttpResponse(status = StatusCodes.OK, entity = HttpEntity(Content))
      val futureResult = aviationFlow.extractBody(response.entity, response.status, 1)
      whenReady(futureResult) { _ shouldBe Content }
    }
    "return the body for any incorrect response" in {
      val response     = HttpResponse(status = StatusCodes.BadRequest, entity = HttpEntity(Content))
      val futureResult = aviationFlow.extractBody(response.entity, response.status, 1)
      whenReady(futureResult) { _ shouldBe Content }
    }
  }

  "unmarshal method" should {
    "parse a flight JSON message into FlightMessageJson" in {
      val futureResult = aviationFlow.unmarshalBody(readFixture("flight"))
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[FlightMessageJson]
      }
    }
    "parse a airplane JSON message into AirplaneMessageJson" in {
      val futureResult = aviationFlow.unmarshalBody(readFixture("airplaneDatabase"))
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[AirplaneMessageJson]
      }
    }
    "parse a airport JSON message into AirportMessageJson" in {
      val futureResult = aviationFlow.unmarshalBody(readFixture("airportDatabase"))
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[AirportMessageJson]
      }
    }
    "parse a airline JSON message into AirlineMessageJson" in {
      val futureResult = aviationFlow.unmarshalBody(readFixture("airlineDatabase"))
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[AirlineMessageJson]
      }
    }
    "parse a city JSON message into CityMessageJson" in {
      val futureResult = aviationFlow.unmarshalBody(readFixture("cityDatabase"))
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[CityMessageJson]
      }
    }
    "create an ErrorMessageJson with the field failedJson equals to the response body" in {
      val futureResult = aviationFlow.unmarshalBody(ErrorResponse)
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isLeft shouldBe true
        result.head.left.value.failedJson shouldBe ErrorResponse
      }
    }
    "create an ErrorMessageJson if at least one of the fields of the response is incorrect" in {
      val futureResult = aviationFlow.unmarshalBody(incorrectJsonAirline)
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isLeft shouldBe true
      }
    }
  }

  private def readFixture(fixtureName: String): String =
    scala.io.Source.fromResource(s"fixtures/aviation-edge-api/$fixtureName.json").mkString

}
