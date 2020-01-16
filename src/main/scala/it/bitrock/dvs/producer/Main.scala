package it.bitrock.dvs.producer

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.dvs.producer.model._
import it.bitrock.dvs.producer.services.MainFunctions._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Main extends App with LazyLogging {

  implicit val system: ActorSystem  = ActorSystem("KafkaDVSProducer")
  implicit val ec: ExecutionContext = system.dispatcher

  logger.info("Starting up")

  val bindingFuture = bindRoutes()

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
      _       <- system.terminate()
    } yield ()
    Await.result(resourcesClosed, 10.seconds)
  }

}
