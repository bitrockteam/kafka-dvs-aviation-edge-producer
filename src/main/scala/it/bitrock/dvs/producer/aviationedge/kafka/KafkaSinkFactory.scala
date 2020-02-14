package it.bitrock.dvs.producer.aviationedge.kafka

import akka.Done
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, Keep, Sink}
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Future

class KafkaSinkFactory[J, K, V](
    val topic: String,
    producerSettings: ProducerSettings[K, V]
)(implicit toValuePair: ToValuePair[J, K, V]) {
  def sink: Sink[J, Future[Done]] =
    Flow
      .fromFunction(
        toValuePair.toValuePair
      )
      .map {
        case (k, v) =>
          new ProducerRecord[K, V](topic, k, v)
      }
      .toMat(Producer.plainSink(producerSettings))(Keep.right)
}
