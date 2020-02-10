package it.bitrock.dvs.producer.aviationedge.model

import java.time.Instant

sealed trait AviationStream
case object FlightStream   extends AviationStream
case object AirplaneStream extends AviationStream
case object AirportStream  extends AviationStream
case object AirlineStream  extends AviationStream
case object CityStream     extends AviationStream

case class Tick()

case class ErrorMessageJson(
    errorSource: String,
    errorMessage: String,
    failedJson: String,
    timestamp: Instant
)

case class MonitoringMessageJson(
    messageReceivedOn: Instant,
    minUpdated: Instant,
    maxUpdated: Instant,
    numErrors: Int,
    numValid: Int,
    numInvalid: Int
)

object PartitionPorts {
  final val RawPort     = 0
  final val ErrorPort   = 1
  final val InvalidPort = 2
}
