package it.bitrock.kafkaflightstream.producer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.kafkaflightstream.producer.model._
import it.bitrock.kafkaflightstream.producer.services.MainFunctions._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Main extends App with LazyLogging {

  implicit val system: ActorSystem    = ActorSystem("KafkaFlightstreamProducer")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext   = system.dispatcher

  logger.info("Starting up")

  val bindingFuture = openHealthCheckPort()

  bindingFuture.map { serverBinding =>
    logger.info(s"Exposing to ${serverBinding.localAddress}")
  }

  val cancellableFlight   = runStream[FlightStream.type]()
  val cancellableAirplane = runStream[AirplaneStream.type]()
  val cancellableAirport  = runStream[AirportStream.type]()
  val cancellableAirline  = runStream[AirlineStream.type]()
  val cancellableCity     = runStream[CityStream.type]()

  sys.addShutdownHook {
    logger.info("Shutting down")
    cancellableFlight.cancel()
    cancellableAirplane.cancel()
    cancellableAirport.cancel()
    cancellableAirline.cancel()
    cancellableCity.cancel()
    val resourcesClosed = for {
      binding <- bindingFuture
      _       <- binding.terminate(hardDeadline = 3.seconds)
      t       <- system.terminate()
    } yield t
    Await.result(resourcesClosed, 10.seconds)
  }

}
