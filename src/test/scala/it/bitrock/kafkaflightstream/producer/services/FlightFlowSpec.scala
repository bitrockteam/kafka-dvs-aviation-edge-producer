package it.bitrock.kafkaflightstream.producer.services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import it.bitrock.kafkaflightstream.producer.model.FlightMessageJson
import it.bitrock.kafkaflightstream.producer.services.FlightFlowSpec.Resource
import it.bitrock.kafkageostream.testcommons.{FixtureLoanerAnyResult, Suite}
import org.scalatest.WordSpecLike

import scala.concurrent.duration._

class FlightFlowSpec extends TestKit(ActorSystem("FlightFlowSpec")) with Suite with WordSpecLike {
  implicit val mat: ActorMaterializer = ActorMaterializer()

  "flow method" should {

    "parse a JSON message into the domain model" in ResourceLoaner.withFixture {
      case Resource(flightFlow, sinkProbe) =>
        val result = Source
          .single(Tick())
          .via(flightFlow.flow("https://aviation-edge.com/v2/public/flights?key=896165-8a7104&limit=1"))
          .mapConcat(identity)
          .toMat(sinkProbe)(Keep.right)
          .run()

        val expectedValue = result.requestNext(10 seconds)

        expectedValue shouldBe a[FlightMessageJson]

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
      val flowFactory = new FlightFlow()
      val sinkProbe   = TestSink.probe[FlightMessageJson]

      body(
        Resource(
          flowFactory,
          sinkProbe
        )
      )
    }
  }

}

object FlightFlowSpec {

  final case class Resource(
      flightFlow: FlightFlow,
      sinkProbe: Sink[FlightMessageJson, Probe[FlightMessageJson]]
  )

}
