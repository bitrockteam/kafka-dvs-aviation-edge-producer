package it.bitrock.kafkaflightstream.producer.services

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import it.bitrock.kafkaflightstream.producer.stream.SourceFactory

import scala.concurrent.duration._

class TickSource(start: FiniteDuration, interval: FiniteDuration) extends SourceFactory[Tick, Cancellable] {

  def source: Source[Tick, Cancellable] = Source.tick(start, interval, Tick())

}

case class Tick()
