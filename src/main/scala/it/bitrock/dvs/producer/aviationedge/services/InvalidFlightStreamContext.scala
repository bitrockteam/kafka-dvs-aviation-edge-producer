package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import it.bitrock.dvs.producer.aviationedge.config.{AviationConfig, AviationStreamConfig, KafkaConfig}
import it.bitrock.dvs.producer.aviationedge.kafka.KafkaTypes.{Flight, Key}
import it.bitrock.dvs.producer.aviationedge.kafka.{KafkaSinkFactory, ProducerSettingsFactory}
import it.bitrock.dvs.producer.aviationedge.model.{FlightStream, MessageJson}

import scala.concurrent.Future

object InvalidFlightStreamContext extends AviationStreamContext[FlightStream.type] {
  override def config(aviationConfig: AviationConfig): AviationStreamConfig = aviationConfig.flightStream

  override def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]] =  {
    val producerSettings = ProducerSettingsFactory.from[Flight.Value](kafkaConfig)
    new KafkaSinkFactory[MessageJson, Key, Flight.Value](kafkaConfig.invalidFlightRawTopic, producerSettings).sink
  }
}
