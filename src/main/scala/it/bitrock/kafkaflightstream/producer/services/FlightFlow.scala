package it.bitrock.kafkaflightstream.producer.services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.kafkaflightstream.producer.model.{FlightMessageJson}
import it.bitrock.kafkaflightstream.producer.stream.JsonSupport._

class FlightFlow()(implicit system: ActorSystem, materializer: ActorMaterializer) extends LazyLogging {

  import system.dispatcher

  def requestFlow(uri: Uri): Flow[Tick, ResponseEntity, NotUsed] = {
    Flow
      .fromFunction((x: Tick) => x)
      .mapAsync(1) { _ =>
        Http().singleRequest(HttpRequest(HttpMethods.GET, uri)).map {
          case HttpResponse(StatusCodes.OK, _, entity, _) =>
            entity.withContentType(ContentTypes.`application/json`)
          case HttpResponse(statusCode, _, _, _) =>
            logger.error(s"Unexpected status code $statusCode")
            HttpEntity.Empty
        }
      }
  }

  def unmarshalFlow: Flow[ResponseEntity, List[FlightMessageJson], NotUsed] = {
    Flow
      .fromFunction((x: ResponseEntity) => x)
      .mapAsync(1) { entity =>
        Unmarshal(entity).to[List[FlightMessageJson]]
      // aggiungere recover in caso di entity vuota
      }
  }
}
