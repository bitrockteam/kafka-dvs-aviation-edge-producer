package it.bitrock.dvs.producer.aviationedge.services

import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.model.MessageJson
import it.bitrock.testcommons.Suite
import org.scalatest.WordSpecLike

class FilterFunctionsSpec extends Suite with WordSpecLike with TestValues {

  import MainFunctions._

  "filter function" should {

    "exclude airlines with status not equal to active" in {
      val messages: List[MessageJson] = List(ValidAirlineMessage, InvalidAirlineMessage)
      messages.count(filterFunction) shouldBe 1
    }

    "exclude flights with status not equal to en-route" in {
      val messages: List[MessageJson] = List(
        StartedFlightMessage,
        EnRouteFlightMessage,
        LandedFlightMessage,
        UnknownFlightMessage,
        CrashedFlightMessage
      )
      messages.count(filterFunction) shouldBe 1
    }

    "exclude flights with invalid speed" in {
      val messages: List[MessageJson] = List(FlightMessage, InvalidSpeedFlightMessage)
      messages.count(filterFunction) shouldBe 1
    }

    "exclude flights with invalid departure or arrival airport" in {
      val messages: List[MessageJson] = List(FlightMessage, InvalidDepartureFlightMessage, InvalidArrivalFlightMessage)
      messages.count(filterFunction) shouldBe 1
    }

  }

}
