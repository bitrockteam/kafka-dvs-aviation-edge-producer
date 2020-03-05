package it.bitrock.dvs.producer.aviationedge.services

import it.bitrock.dvs.producer.aviationedge.TestValues._
import it.bitrock.dvs.producer.aviationedge.model.MessageJson
import it.bitrock.dvs.producer.aviationedge.model.PartitionPorts._
import it.bitrock.dvs.producer.aviationedge.services.Graphs._
import it.bitrock.testcommons.Suite
import org.scalatest.wordspec.AnyWordSpecLike

class FilterFunctionsSpec extends Suite with AnyWordSpecLike {

  "filter function" should {
    "exclude airlines with status not equal to active" in {
      val messages: List[MessageJson] = List(ValidAirlineMessage, InvalidAirlineMessage)
      messages.count(filterFunction) shouldBe 1
    }
  }

  "partitionMessages" should {
    "return ErrorPort with an error event" in {
      partitionMessages(Left(ErrorMessage)) shouldBe ErrorPort
    }
    "return RawPort whatever the flight status is" in {
      partitionMessages(Right(CrashedFlightMessage)) shouldBe RawPort
      partitionMessages(Right(EnRouteFlightMessage)) shouldBe RawPort
      partitionMessages(Right(LandedFlightMessage)) shouldBe RawPort
      partitionMessages(Right(StartedFlightMessage)) shouldBe RawPort
      partitionMessages(Right(UnknownFlightMessage)) shouldBe RawPort
    }
    "return RawPort with a valid airline event" in {
      partitionMessages(Right(ValidAirlineMessage)) shouldBe RawPort
    }
    "return InvalidPort with an invalid flight event" in {
      partitionMessages(Right(InvalidSpeedFlightMessage)) shouldBe InvalidPort
      partitionMessages(Right(InvalidDepartureFlightMessage)) shouldBe InvalidPort
      partitionMessages(Right(InvalidArrivalFlightMessage)) shouldBe InvalidPort
    }
  }
}
