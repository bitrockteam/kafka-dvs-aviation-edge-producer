package it.bitrock.dvs.producer.aviationedge.services

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.TestKit
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.model.{ErrorMessageJson, MessageJson, MonitoringMessageJson}
import it.bitrock.dvs.producer.aviationedge.services.Graphs._
import it.bitrock.testcommons.Suite
import net.manub.embeddedkafka.schemaregistry._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration._

class GraphsSpec
    extends TestKit(ActorSystem("GraphsSpec"))
    with Suite
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with EmbeddedKafka
    with TestValues
    with ScalaFutures
    with LazyLogging {

  private val timeout = Timeout(3.seconds)

  "graphs" should {

    "route error, valid and invalid messages to different sinks" in {
      val source =
        Source(List(Right(FlightMessage), Left(ErrorMessage), Right(UnknownFlightMessage), Right(InvalidSpeedFlightMessage)))
      val flightSink        = Sink.fold[List[MessageJson], MessageJson](Nil)(_ :+ _)
      val errorSink         = Sink.fold[List[ErrorMessageJson], ErrorMessageJson](Nil)(_ :+ _)
      val invalidFlightSink = Sink.fold[List[MessageJson], MessageJson](Nil)(_ :+ _)

      val (_, futureFlight, futureError, futureInvalidFlight) = mainGraph(source, flightSink, errorSink, invalidFlightSink).run()

      whenReady(futureFlight, timeout) { f =>
        f.size shouldBe 2
        f should contain theSameElementsAs List(FlightMessage, UnknownFlightMessage)
      }
      whenReady(futureError, timeout) { e =>
        e.size shouldBe 1
        e.head shouldBe ErrorMessage
      }
      whenReady(futureInvalidFlight, timeout) { e =>
        e.size shouldBe 1
        e.head shouldBe InvalidSpeedFlightMessage
      }
    }

    "produce monitoring messages to monitoring sink" in {
      val source = Source.single(
        List(
          Right(FlightMessage),
          Right(UnknownFlightMessage),
          Left(ErrorMessage),
          Right(ValidAirlineMessage),
          Right(MaxUpdatedFlightMessage),
          Left(ErrorMessage.copy(errorSource = "/v2/public/flights")),
          Right(InvalidDepartureFlightMessage),
          Right(MinUpdatedFlightMessage)
        )
      )
      val monitoringSink: Sink[MonitoringMessageJson, Future[List[MonitoringMessageJson]]] =
        Sink.fold[List[MonitoringMessageJson], MonitoringMessageJson](Nil)(_ :+ _)

      val futureMonitoring = source.viaMat(monitoringGraph(monitoringSink))(Keep.right).to(Sink.ignore).run()

      whenReady(futureMonitoring, timeout) { m =>
        m.size shouldBe 1
        m.head.minUpdated shouldBe Instant.ofEpochSecond(MinUpdated)
        m.head.maxUpdated shouldBe Instant.ofEpochSecond(MaxUpdated)
        m.head.averageUpdated shouldBe Instant.ofEpochSecond((MinUpdated + MaxUpdated + Updated) / 3)
        m.head.numErrors shouldBe 1
        m.head.numValid shouldBe 4
        m.head.numInvalid shouldBe 1
        m.head.total shouldBe 6
      }
    }

  }

}
