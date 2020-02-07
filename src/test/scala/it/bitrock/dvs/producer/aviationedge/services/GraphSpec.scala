package it.bitrock.dvs.producer.aviationedge.services

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.model.{ErrorMessageJson, MessageJson}
import it.bitrock.testcommons.Suite
import net.manub.embeddedkafka.schemaregistry._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike

class GraphSpec
    extends TestKit(ActorSystem("GraphSpec"))
    with Suite
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with EmbeddedKafka
    with TestValues
    with ScalaFutures
    with LazyLogging {

  import MainFunctions.buildGraph

  "graph method" should {

    "routes error messages and correct messages to different sinks" in {

      val source            = Source(List(Right(FlightMessage), Left(ErrorMessage), Right(UnknownFlightMessage)))
      val flightSink        = Sink.fold[List[MessageJson], MessageJson](Nil)(_ :+ _)
      val errorSink         = Sink.fold[List[ErrorMessageJson], ErrorMessageJson](Nil)(_ :+ _)
      val invalidFlightSink = Sink.fold[List[MessageJson], MessageJson](Nil)(_ :+ _)

      val (_, futureFlight, futureError, futureInvalidFlight) = buildGraph(source, flightSink, errorSink, invalidFlightSink).run()

      whenReady(futureFlight) { f =>
        f.size shouldBe 1
        f.head shouldBe FlightMessage
      }
      whenReady(futureError) { e =>
        e.size shouldBe 1
        e.head shouldBe ErrorMessage
      }
      whenReady(futureInvalidFlight) { e =>
        e.size shouldBe 1
        e.head shouldBe UnknownFlightMessage
      }
    }
  }
}
