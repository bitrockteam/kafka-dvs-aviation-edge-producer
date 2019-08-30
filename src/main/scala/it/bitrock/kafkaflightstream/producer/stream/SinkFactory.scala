package it.bitrock.kafkaflightstream.producer.stream

import akka.Done
import akka.stream.scaladsl.Sink

import scala.concurrent.Future

trait SinkFactory[J] {

  def sink: Sink[J, Future[Done]]

}
