package it.bitrock.dvs.producer.services

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import it.bitrock.dvs.producer.model._
import AviationFlowSpec.Resource
import it.bitrock.testcommons.{FixtureLoanerAnyResult, Suite}
import org.scalatest.WordSpecLike

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AviationFlowSpec extends TestKit(ActorSystem("AviationFlowSpec")) with Suite with WordSpecLike {

  "flow method" should {

    "parse a flight JSON message into FlightMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("flights")))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.requestNext(20.seconds) shouldBe a[FlightMessageJson]
    }

    "parse a airplane JSON message into AirplaneMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("airplaneDatabase")))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.requestNext(60.seconds) shouldBe a[AirplaneMessageJson]
    }

    "parse a airport JSON message into AirportMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("airportDatabase")))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.requestNext(20.seconds) shouldBe a[AirportMessageJson]
    }

    "parse a airline JSON message into AirlineMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("airlineDatabase")))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.requestNext(20.seconds) shouldBe a[AirlineMessageJson]
    }

    "parse a city JSON message into CityMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(aviationFlow.flow(() => readFixture("cityDatabase")))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.requestNext(20.seconds) shouldBe a[CityMessageJson]
    }

    "complete immediately for an incorrect request" in ResourceLoaner.withFixture {
      case Resource(flightFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(flightFlow.flow(() => readFixture("invalidApiKey")))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.expectSubscriptionAndComplete()
    }

    "complete immediately when the API provides some malformed JSON" in ResourceLoaner.withFixture {
      case Resource(flightFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(flightFlow.flow(() => Future("this-is-not-a-JSON-document")))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.expectSubscriptionAndComplete()
    }
  }

  object ResourceLoaner extends FixtureLoanerAnyResult[Resource] {
    override def withFixture(body: Resource => Any): Any = {
      val aviationFlow = new AviationFlow()
      val sinkProbe    = TestSink.probe[MessageJson]

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
      sinkProbe: Sink[MessageJson, Probe[MessageJson]]
  )
}
