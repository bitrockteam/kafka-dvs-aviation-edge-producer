package it.bitrock.dvs.producer.aviationedge

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.dvs.producer.aviationedge.model._
import it.bitrock.dvs.producer.aviationedge.services.MainFunctions._
import it.bitrock.dvs.producer.aviationedge.services.context.AviationStreamContext._
import it.bitrock.dvs.producer.aviationedge.services.context.OpenSkyStreamContext._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object Main extends App with LazyLogging {
  implicit val system: ActorSystem  = ActorSystem("KafkaDVSAviationEdgeProducer")
  implicit val ec: ExecutionContext = system.dispatcher

  logger.info("Starting up")

  val bindingFuture = bindRoutes()

  bindingFuture.map(serverBinding => logger.info(s"Exposing to ${serverBinding.localAddress}"))

  val (cancellableFlight, flightCompletion)           = runAviationEdgeStream[FlightStream.type]()
  val (cancellableAirplane, airplaneCompletion)       = runAviationEdgeStream[AirplaneStream.type]()
  val (cancellableAirport, airportCompletion)         = runAviationEdgeStream[AirportStream.type]()
  val (cancellableAirline, airlineCompletion)         = runAviationEdgeStream[AirlineStream.type]()
  val (cancellableCity, cityCompletion)               = runAviationEdgeStream[CityStream.type]()
  val (cancellableFlightState, flightStateCompletion) = runOpenSkyStream[FlightStateStream.type]()

  val streamsCompletion =
    List(flightCompletion, airplaneCompletion, airportCompletion, airlineCompletion, cityCompletion, flightStateCompletion)
  Future.firstCompletedOf(streamsCompletion).onComplete { _ =>
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
    cancellableFlightState.cancel()
    val resourcesClosed = for {
      binding <- bindingFuture
      _       <- binding.terminate(hardDeadline = 3.seconds)
      _       <- system.terminate()
    } yield ()
    Await.result(resourcesClosed, 10.seconds)
  }
}
