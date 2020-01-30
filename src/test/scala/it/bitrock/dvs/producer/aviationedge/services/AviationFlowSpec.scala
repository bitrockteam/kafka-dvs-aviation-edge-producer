package it.bitrock.dvs.producer.aviationedge.services

import AviationFlowSpec.Resource
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.testcommons.{FixtureLoanerAnyResult, Suite}
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AviationFlowSpec extends TestKit(ActorSystem("AviationFlowSpec")) with Suite with AnyWordSpecLike with EitherValues {

  "flow method" should {

    "parse a flight JSON message into FlightMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val stream = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("flights")))
          .toMat(sinkProbe)(Keep.right)
          .run()

        val result = stream.requestNext

        result.size shouldBe 1
        result.head.right.value shouldBe a[FlightMessageJson]
    }

    "parse a airplane JSON message into AirplaneMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val stream = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("airplaneDatabase")))
          .toMat(sinkProbe)(Keep.right)
          .run()

        val result = stream.requestNext

        result.size shouldBe 1
        result.head.right.value shouldBe a[AirplaneMessageJson]
    }

    "parse a airport JSON message into AirportMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val stream = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("airportDatabase")))
          .toMat(sinkProbe)(Keep.right)
          .run()

        val result = stream.requestNext

        result.size shouldBe 1
        result.head.right.value shouldBe a[AirportMessageJson]
    }

    "parse a airline JSON message into AirlineMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val stream = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("airlineDatabase")))
          .toMat(sinkProbe)(Keep.right)
          .run()

        val result = stream.requestNext

        result.size shouldBe 1
        result.head.right.value shouldBe a[AirlineMessageJson]
    }

    "parse a city JSON message into CityMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val stream = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("cityDatabase")))
          .toMat(sinkProbe)(Keep.right)
          .run()

        val result = stream.requestNext

        result.size shouldBe 1
        result.head.right.value shouldBe a[CityMessageJson]
    }

    "complete immediately for an incorrect request" in ResourceLoaner.withFixture {
      case Resource(flightFlow, sinkProbe) =>
        val stream = Source
          .single(Tick())
          .via(flightFlow.flow(() => readFixture("invalidApiKey")))
          .toMat(sinkProbe)(Keep.right)
          .run()

        val result = stream.requestNext
        result.head.left.value shouldBe a[ErrorMessageJson]
    }

    "complete immediately when the API provides some malformed JSON" in ResourceLoaner.withFixture {
      case Resource(flightFlow, sinkProbe) =>
        val stream = Source
          .single(Tick())
          .via(flightFlow.flow(() => Future("this-is-not-a-JSON-document")))
          .toMat(sinkProbe)(Keep.right)
          .run()

        val result = stream.requestNext
        result.head.left.value shouldBe a[ErrorMessageJson]
    }

  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      val aviationFlow = new AviationFlow()
      val sinkProbe    = TestSink.probe[List[Either[ErrorMessageJson, MessageJson]]]

      body(
        Resource(
          aviationFlow,
          sinkProbe
        )
      )
    }
  }

  private def readFixture(fixtureName: String): Future[String] = Future {
    scala.io.Source
      .fromResource(s"fixtures/aviation-edge-api/$fixtureName.json", this.getClass.getClassLoader)
      .mkString
  }
}

object AviationFlowSpec {
  final case class Resource(
      aviationFlow: AviationFlow,
      sinkProbe: Sink[List[Either[ErrorMessageJson, MessageJson]], Probe[List[Either[ErrorMessageJson, MessageJson]]]]
  )
}
