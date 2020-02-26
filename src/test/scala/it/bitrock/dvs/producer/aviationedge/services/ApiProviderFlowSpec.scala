package it.bitrock.dvs.producer.aviationedge.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes, Uri}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.TestKit
import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.dvs.producer.aviationedge.services.JsonSupport._
import it.bitrock.testcommons.Suite
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ApiProviderFlowSpec
    extends TestKit(ActorSystem("AviationFlowSpec"))
    with Suite
    with AnyWordSpecLike
    with TestValues
    with EitherValues
    with ScalaFutures
    with IntegrationPatience {
  private val apiProviderFlow = new ApiProviderFlow()

  "flow method" should {
    "recover http request failure" in {
      val flow = apiProviderFlow.flow(Uri("invalid-url"), 1)(responsePayloadJsonFormat)
      whenReady(Source.tick(0.seconds, 1.second, Tick()).via(flow).take(1).toMat(Sink.head)(Keep.right).run()) { result =>
        result.head.left.value.errorSource shouldBe "invalid-url"
      }
    }
  }

  "extract method" should {
    "return the body for any correct response" in {
      val response     = HttpResponse(status = StatusCodes.OK, entity = HttpEntity(Content))
      val futureResult = apiProviderFlow.extractBody(response.entity, response.status, 1)
      whenReady(futureResult)(_ shouldBe Content)
    }
    "return the body for any incorrect response" in {
      val response     = HttpResponse(status = StatusCodes.BadRequest, entity = HttpEntity(Content))
      val futureResult = apiProviderFlow.extractBody(response.entity, response.status, 1)
      whenReady(futureResult)(_ shouldBe Content)
    }
  }

  "unmarshal method" should {
    "parse a flight JSON message into FlightMessageJson" in {
      val futureResult = apiProviderFlow.unmarshalBody(readFixture("flight"), Path)(responsePayloadJsonFormat)
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[FlightMessageJson]
      }
    }
    "parse a airplane JSON message into AirplaneMessageJson" in {
      val futureResult = apiProviderFlow.unmarshalBody(readFixture("airplaneDatabase"), Path)(responsePayloadJsonFormat)
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[AirplaneMessageJson]
      }
    }
    "parse a airport JSON message into AirportMessageJson" in {
      val futureResult = apiProviderFlow.unmarshalBody(readFixture("airportDatabase"), Path)(responsePayloadJsonFormat)
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[AirportMessageJson]
      }
    }
    "parse a airline JSON message into AirlineMessageJson" in {
      val futureResult = apiProviderFlow.unmarshalBody(readFixture("airlineDatabase"), Path)(responsePayloadJsonFormat)
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[AirlineMessageJson]
      }
    }
    "parse a city JSON message into CityMessageJson" in {
      val futureResult = apiProviderFlow.unmarshalBody(readFixture("cityDatabase"), Path)(responsePayloadJsonFormat)
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[CityMessageJson]
      }
    }
    "parse a flight states JSON message into FlightStateJson" in {
      val futureResult =
        apiProviderFlow.unmarshalBody(readFixture("flightStatesDatabase"), Path)(openSkyResponsePayloadJsonFormat)
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isRight shouldBe true
        result.head.right.value shouldBe a[FlightStateJson]
      }
    }
    "create an ErrorMessageJson with the field failedJson equals to the response body" when {
      "the provider is aviation-edge" in {
        val futureResult = apiProviderFlow.unmarshalBody(ErrorResponse, Path)(responsePayloadJsonFormat)
        whenReady(futureResult) { result =>
          result.size shouldBe 1
          result.head.isLeft shouldBe true
          result.head.left.value.failedJson shouldBe ErrorResponse
        }
      }
      "the provider is open-sky" in {
        val futureResult = apiProviderFlow.unmarshalBody(ErrorResponse, Path)(openSkyResponsePayloadJsonFormat)
        whenReady(futureResult) { result =>
          result.size shouldBe 1
          result.head.isLeft shouldBe true
          result.head.left.value.failedJson shouldBe ErrorResponse
        }
      }
    }
    "create an ErrorMessageJson if at least one of the fields of the response is incorrect" in {
      val futureResult = apiProviderFlow.unmarshalBody(IncorrectJsonAirline, Path)(responsePayloadJsonFormat)
      whenReady(futureResult) { result =>
        result.size shouldBe 1
        result.head.isLeft shouldBe true
        result.head.left.value.errorSource shouldBe Path
      }
    }
  }

  private def readFixture(fixtureName: String): String =
    scala.io.Source.fromResource(s"fixtures/aviation-edge-api/$fixtureName.json").mkString
}
