package it.bitrock.kafkaflightstream.producer.services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.kafkaflightstream.producer.model.FlightMessageJson
import it.bitrock.kafkaflightstream.producer.stream.JsonSupport._

import scala.concurrent.Future

class FlightClient()(implicit system: ActorSystem, materializer: ActorMaterializer) extends LazyLogging {

  import system.dispatcher

  def flightRequest: Flow[HttpRequest, Future[List[FlightMessageJson]], NotUsed] = {
    Flow.fromFunction(
      request =>
        Http().singleRequest(request).flatMap {
          case HttpResponse(StatusCodes.OK, _, entity, _) =>
            val responseEntityModified = entity.withContentType(ContentTypes.`application/json`)
            Unmarshal(responseEntityModified).to[List[FlightMessageJson]]
          case HttpResponse(statusCode, _, _, _) =>
            logger.error(s"Unexpected status code $statusCode")
            Future(List[FlightMessageJson]())
        }
    )
  }

}
