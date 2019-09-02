package it.bitrock.kafkaflightstream.producer.stream

import akka.stream.scaladsl.Source

trait SourceFactory[J, K] {

  def source: Source[J, K]

}
