package it.bitrock.dvs.producer.aviationedge.services

import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import it.bitrock.dvs.producer.aviationedge.config.TickSourceConfig
import it.bitrock.dvs.producer.aviationedge.model.Tick

import scala.concurrent.duration._

class TickSource(start: Int, interval: Int, config: TickSourceConfig) {

  def source: Source[Tick, Cancellable] = Source.tick(start.seconds, interval.seconds, Tick()).filter(_ => isItTimeToPoll)

  private def isItTimeToPoll: Boolean = {
    val now           = LocalDateTime.now(ZoneOffset.UTC)
    val currentHour   = now.getHour
    val isExcludedDay = config.pollExcludedDays.contains(now.getDayOfWeek)
    !isExcludedDay && (config.pollLowerHourLimit to config.pollUpperHourLimit).contains(currentHour)
  }

}
