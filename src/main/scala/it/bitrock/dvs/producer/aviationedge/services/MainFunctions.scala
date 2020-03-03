package it.bitrock.dvs.producer.aviationedge.services

import akka.Done
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import it.bitrock.dvs.producer.aviationedge.config._
import it.bitrock.dvs.producer.aviationedge.routes.Routes
import it.bitrock.dvs.producer.aviationedge.services.Graphs._
import it.bitrock.dvs.producer.aviationedge.services.JsonSupport._
import it.bitrock.dvs.producer.aviationedge.services.context.{
  ApiProviderStreamContext,
  AviationStreamContext,
  OpenSkyStreamContext
}
import spray.json.JsonReader

import scala.concurrent.{ExecutionContext, Future}

object MainFunctions {
  val serverConfig: ServerConfig           = AppConfig.server
  val apiProviderConfig: ApiProviderConfig = AppConfig.apiProvider
  val aviationConfig: AviationConfig       = apiProviderConfig.aviationEdge
  val openSkyConfig: OpenSkyConfig         = apiProviderConfig.openSky
  val kafkaConfig: KafkaConfig             = AppConfig.kafka

  def bindRoutes()(implicit system: ActorSystem): Future[Http.ServerBinding] = {
    val host   = serverConfig.host
    val port   = serverConfig.port
    val routes = new Routes(serverConfig)
    Http().bindAndHandle(routes.routes, host, port)
  }

  def runAviationEdgeStream[A: ApiProviderStreamContext: JsonReader]()(
      implicit system: ActorSystem,
      ec: ExecutionContext
  ): (Cancellable, Future[Done], Future[Done], Future[Done]) = {
    val config = AviationStreamContext[A].config(apiProviderConfig)

    val tickSource =
      new TickSource(config.pollingStart, config.pollingInterval, aviationConfig.tickSource).source
    val aviationFlow =
      new ApiProviderFlow().flow(aviationConfig.getAviationUri(config.path), aviationConfig.apiTimeout)(
        aviationEdgePayloadJsonReader[A]
      )
    val rawSink           = AviationStreamContext[A].sink(kafkaConfig)
    val errorSink         = SideStreamContext.errorSink(kafkaConfig)
    val invalidFlightSink = SideStreamContext.invalidFlightSink(kafkaConfig)
    val monitoringSink    = SideStreamContext.monitoringSink(kafkaConfig)

    val jsonSource = tickSource.via(aviationFlow).via(monitoringGraph(monitoringSink)).mapConcat(identity)

    mainGraph(jsonSource, rawSink, errorSink, invalidFlightSink).run()
  }

  def runOpenSkyStream[A: ApiProviderStreamContext: JsonReader]()(
      implicit system: ActorSystem,
      ec: ExecutionContext
  ): (Cancellable, Future[Done]) = {
    val config = OpenSkyStreamContext[A].config(apiProviderConfig)

    val tickSource = new TickSource(config.pollingStart, config.pollingInterval, openSkyConfig.tickSource).source
    val openSkyFlow = new ApiProviderFlow().flow(openSkyConfig.getOpenSkyUri(config.path), openSkyConfig.apiTimeout)(
      openSkyResponsePayloadJsonFormat[A]
    )
    val rawSink = AviationStreamContext[A].sink(kafkaConfig)

    val jsonSource = tickSource.via(openSkyFlow).mapConcat(identity)

    collectRightMessagesGraph(jsonSource, rawSink).run()
  }
}
