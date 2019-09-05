package it.bitrock.kafkaflightstream.producer.services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import it.bitrock.kafkaflightstream.producer.model._
import it.bitrock.kafkaflightstream.producer.services.AviationFlowSpec.Resource
import it.bitrock.kafkageostream.testcommons.{FixtureLoanerAnyResult, Suite}
import org.scalatest.WordSpecLike

import scala.concurrent.duration._

class AviationFlowSpec extends TestKit(ActorSystem("AviationFlowSpec")) with Suite with WordSpecLike {
  implicit val mat: ActorMaterializer = ActorMaterializer()

  "flow method" should {

    "parse a flight JSON message into FlightMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(aviationFlow.flow("https://aviation-edge.com/v2/public/flights?key=896165-8a7104&limit=50"))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.requestNext(20.seconds) shouldBe a[FlightMessageJson]

    }

    "parse a airport JSON message into AirportMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(aviationFlow.flow("https://aviation-edge.com/v2/public/airportDatabase?key=896165-8a7104"))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.requestNext(20.seconds) shouldBe a[AirportMessageJson]

    }

    "parse a airline JSON message into AirlineMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(aviationFlow.flow("https://aviation-edge.com/v2/public/airlineDatabase?key=896165-8a7104"))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.requestNext(20.seconds) shouldBe a[AirlineMessageJson]

    }

    "parse a city JSON message into CityMessageJson" in ResourceLoaner.withFixture {
      case Resource(aviationFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(aviationFlow.flow("https://aviation-edge.com/v2/public/cityDatabase?key=896165-8a7104"))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        result.requestNext(20.seconds) shouldBe a[CityMessageJson]

    }

    "complete immediately for an incorrect request" in ResourceLoaner.withFixture {
      case Resource(flightFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(flightFlow.flow("https://aviation-edge.com/v2/public/flights"))
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

}

object AviationFlowSpec {

  final case class Resource(
      aviationFlow: AviationFlow,
      sinkProbe: Sink[MessageJson, Probe[MessageJson]]
  )

}
