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

class FlightFlow()(implicit system: ActorSystem, materializer: ActorMaterializer) extends LazyLogging {

  import system.dispatcher

  def flow(uri: Uri): Flow[Tick, List[FlightMessageJson], NotUsed] = {
    Flow
      .fromFunction((x: Tick) => x)
      .mapAsync(1) { _ =>
        Http().singleRequest(HttpRequest(HttpMethods.GET, uri)).flatMap {
          case HttpResponse(StatusCodes.OK, _, entity, _) =>
            val entityModified = entity.withContentType(ContentTypes.`application/json`)
            Unmarshal(entityModified).to[List[FlightMessageJson]].recover {
              case e =>
                logger.warn(s"Unmarshal error ${e.printStackTrace()}")
                List[FlightMessageJson]()
            }
          case HttpResponse(statusCodes, _, _, _) =>
            logger.warn(s"Bad response status code: $statusCodes")
            Future(List[FlightMessageJson]())
        }
      }
  }

}
