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

  def flow(uri: Uri, apiTimeout: Int): Flow[Tick, List[Either[ErrorMessageJson, MessageJson]], NotUsed] =
    Flow
      .fromFunction(identity[Tick])
      .mapAsync(1) { _ =>
        logger.info(s"Trying to call: $uri")
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri))
          .flatMap(response => extractBody(response.entity, response.status, apiTimeout))
          .flatMap(body => unmarshalBody(body, uri.path.toString))
      }

  def extractBody(entity: ResponseEntity, status: StatusCode, timeout: Int): Future[String] = {
    if (status != StatusCodes.OK)
      logger.warn(s"Bad response status code: $status")
    entity.toStrict(timeout.seconds).map(_.data.utf8String)
  }

  def unmarshalBody(apiResponseBody: String, path: String): Future[List[Either[ErrorMessageJson, MessageJson]]] =
    Unmarshal(apiResponseBody)
      .to[List[Either[ErrorMessageJson, MessageJson]]]
      .map(list => addPathToLeft(list, path))
      .recover {
        case ex =>
          List(Left(ErrorMessageJson(path, ex.getMessage, apiResponseBody, Instant.now)))
      }

  private def addPathToLeft(
      list: List[Either[ErrorMessageJson, MessageJson]],
      path: String
  ): List[Either[ErrorMessageJson, MessageJson]] =
    list.map(_.left.map(e => ErrorMessageJson(path, e.errorMessage, e.failedJson, e.timestamp)))

}
