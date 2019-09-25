package it.bitrock.kafkaflightstream.producer.services

import it.bitrock.kafkaflightstream.producer.TestValues
import it.bitrock.kafkaflightstream.producer.model._
import it.bitrock.kafkageostream.testcommons.Suite
import org.scalatest.WordSpecLike

class FilterFunctionsSpec extends Suite with WordSpecLike with TestValues {

  import MainFunctions._

  "filter function" should {

    "exclude airlines with status not equal to active" in {
      val messages: List[MessageJson] = List(ValidAirlineMessage, InvalidAirlineMessage)
      messages.count(filterAirline) shouldBe 1
    }

    "exclude flights with status not equal to started or en-route or landed" in {
      val messages: List[MessageJson] = List(
        StartedFlightMessage,
        EnRouteFlightMessage,
        LandedFlightMessage,
        UnknownFlightMessage,
        CrashedFlightMessage
      )
      messages.count(filterFlight) shouldBe 3
    }

    "exclude flights with invalid speed" in {
      val messages: List[MessageJson] = List(FlightMessage, InvalidSpeedFlightMessage)
      messages.count(filterFlight) shouldBe 1
    }

    "exclude flights with invalid departure or arrival airport" in {
      val messages: List[MessageJson] = List(FlightMessage, InvalidDepartureFlightMessage, InvalidArrivalFlightMessage)
      messages.count(filterFlight) shouldBe 1
    }

  }

}
