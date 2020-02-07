package it.bitrock.dvs.producer.aviationedge

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.dvs.producer.aviationedge.services.MainFunctions._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object Main extends App with LazyLogging {

  implicit val system: ActorSystem  = ActorSystem("KafkaDVSAviationEdgeProducer")
  implicit val ec: ExecutionContext = system.dispatcher

  logger.info("Starting up")

  val bindingFuture = bindRoutes()

  bindingFuture.map { serverBinding =>
    logger.info(s"Exposing to ${serverBinding.localAddress}")
  }

  val (cancellableFlight, flightCompletion, _)     = runStream[FlightStream.type]()
  val (cancellableAirplane, airplaneCompletion, _) = runStream[AirplaneStream.type]()
  val (cancellableAirport, airportCompletion, _)   = runStream[AirportStream.type]()
  val (cancellableAirline, airlineCompletion, _)   = runStream[AirlineStream.type]()
  val (cancellableCity, cityCompletion, _)         = runStream[CityStream.type]()

  val streamsCompletion = List(flightCompletion, airplaneCompletion, airportCompletion, airlineCompletion, cityCompletion)
  Future.firstCompletedOf(streamsCompletion).foreach { _ =>
    logger.error("An unexpected error caused a stream completion. Terminating the application...")
    sys.exit(1)
  }

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
