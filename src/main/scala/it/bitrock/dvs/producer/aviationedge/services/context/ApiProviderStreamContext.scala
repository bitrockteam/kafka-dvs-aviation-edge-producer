package it.bitrock.dvs.producer.aviationedge.services.context

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import it.bitrock.dvs.producer.aviationedge.config.{ApiProviderConfig, ApiProviderStreamConfig, KafkaConfig}
import it.bitrock.dvs.producer.aviationedge.model.MessageJson

import scala.concurrent.Future

trait ApiProviderStreamContext[A] {
  def config(apiProvider: ApiProviderConfig): ApiProviderStreamConfig
  def sink(kafkaConfig: KafkaConfig)(implicit system: ActorSystem): Sink[MessageJson, Future[Done]]
}
