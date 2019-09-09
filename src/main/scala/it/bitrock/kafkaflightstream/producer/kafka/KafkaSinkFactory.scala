package it.bitrock.kafkaflightstream.producer.kafka

import akka.Done
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Future

class KafkaSinkFactory[J, K, V](
    val topic: String,
    producerSettings: ProducerSettings[K, V]
)(implicit toValuePair: ToValuePair[J, K, V]) extends LazyLogging {

  def sink: Sink[J, Future[Done]] =
    Flow
      .fromFunction(
        toValuePair.toValuePair
      )
      .map {
        case (k, v) =>
          logger.info(s"Producing on $topic, the key $k")
          new ProducerRecord[K, V](topic, k, v)
      }
      .toMat(Producer.plainSink(producerSettings))(Keep.right)

}
