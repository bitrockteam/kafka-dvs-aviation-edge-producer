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

    "return 0 with an ErrorMessageJson" in {
      partitionMessages(Left(ErrorMessage)) shouldBe 0
    }

    "return 1 with a valid flight event" in {
      partitionMessages(Right(EnRouteFlightMessage)) shouldBe 1
    }

    "return 2 with an invalid flight event" in {
      partitionMessages(Right(StartedFlightMessage)) shouldBe 2
      partitionMessages(Right(LandedFlightMessage)) shouldBe 2
      partitionMessages(Right(UnknownFlightMessage)) shouldBe 2
      partitionMessages(Right(CrashedFlightMessage)) shouldBe 2
      partitionMessages(Right(InvalidSpeedFlightMessage)) shouldBe 2
      partitionMessages(Right(InvalidDepartureFlightMessage)) shouldBe 2
      partitionMessages(Right(InvalidArrivalFlightMessage)) shouldBe 2
    }

    "return 1 with a valid airline event" in {
      partitionMessages(Right(ValidAirlineMessage)) shouldBe 1
    }
  }
}
