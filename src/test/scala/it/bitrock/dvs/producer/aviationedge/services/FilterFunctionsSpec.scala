package it.bitrock.dvs.producer.aviationedge.services

import it.bitrock.dvs.producer.aviationedge.TestValues
import it.bitrock.dvs.producer.aviationedge.model.MessageJson
import it.bitrock.testcommons.Suite
import org.scalatest.wordspec.AnyWordSpecLike

class FilterFunctionsSpec extends Suite with AnyWordSpecLike with TestValues {
  "function filter" should {
    import MainFunctions.filterFunction

    "exclude airlines with status not equal to active" in {
      val messages: List[MessageJson] = List(ValidAirlineMessage, InvalidAirlineMessage)
      messages.count(filterFunction) shouldBe 1
    }
  }

  "function partitionMessages" should {
    import MainFunctions.partitionMessages

    "return 0 with a valid flight event" in {
      partitionMessages(EnRouteFlightMessage) shouldBe 0
    }

    "return 1 with an invalid flight event" in {
      partitionMessages(StartedFlightMessage) shouldBe 1
      partitionMessages(LandedFlightMessage) shouldBe 1
      partitionMessages(UnknownFlightMessage) shouldBe 1
      partitionMessages(CrashedFlightMessage) shouldBe 1
      partitionMessages(InvalidSpeedFlightMessage) shouldBe 1
      partitionMessages(InvalidDepartureFlightMessage) shouldBe 1
      partitionMessages(InvalidArrivalFlightMessage) shouldBe 1
    }

    "return 0 with a valid airline event" in {
      partitionMessages(ValidAirlineMessage) shouldBe 0
    }
  }
}
