package it.bitrock.dvs.producer.aviationedge.services

import java.time.Instant

import JsonSupport._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.dvs.producer.aviationedge.model.{ErrorMessageJson, MessageJson, Tick}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class AviationFlow()(implicit system: ActorSystem, ec: ExecutionContext) extends LazyLogging {

  def flow(uri: Uri, apiTimeout: Int): Flow[Tick, List[Either[ErrorMessageJson, MessageJson]], NotUsed] = flow { () =>
    logger.info(s"Trying to call: $uri")
    Http().singleRequest(HttpRequest(HttpMethods.GET, uri)).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        entity
          .toStrict(apiTimeout.seconds)
          .map(_.data.utf8String)
      case HttpResponse(statusCodes, _, _, _) =>
        logger.warn(s"Bad response status code: $statusCodes")
        Future.successful("")
    }
  }

  def flow(apiProvider: () => Future[String]): Flow[Tick, List[Either[ErrorMessageJson, MessageJson]], NotUsed] =
    Flow
      .fromFunction((x: Tick) => x)
      .mapAsync(1) { _ =>
        apiProvider().flatMap(response => unmarshal(response))
      }

  private def unmarshal(apiResponseBody: String): Future[List[Either[ErrorMessageJson, MessageJson]]] =
    Unmarshal(apiResponseBody).to[List[Either[ErrorMessageJson, MessageJson]]].recover {
      case ex => List(Left(ErrorMessageJson(ex.getMessage, apiResponseBody, Instant.now)))
    }
}
