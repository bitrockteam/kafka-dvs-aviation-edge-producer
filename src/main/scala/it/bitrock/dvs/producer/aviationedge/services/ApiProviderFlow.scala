package it.bitrock.dvs.producer.aviationedge.services

import java.time.Instant

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.dvs.producer.aviationedge.model.{ErrorMessageJson, Tick}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ApiProviderFlow()(implicit system: ActorSystem, ec: ExecutionContext) extends LazyLogging {
  def flow[A](uri: Uri, apiTimeout: Int)(
      implicit um: Unmarshaller[String, List[Either[ErrorMessageJson, A]]]
  ): Flow[Tick, List[Either[ErrorMessageJson, A]], NotUsed] =
    Flow
      .fromFunction(identity[Tick])
      .mapAsync(1) { _ =>
        logger.info(s"Trying to call: $uri")
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri))
          .flatMap(response => extractBody(response.entity, response.status, apiTimeout))
          .flatMap(body => unmarshalBody(body, uri.path.toString))
          .recover {
            case NonFatal(ex) =>
              logger.warn(s"Error on call: $uri, $ex")
              List(Left(ErrorMessageJson(uri.path.toString, ex.getMessage, "", Instant.now)))
          }
      }

  def extractBody(entity: ResponseEntity, status: StatusCode, timeout: Int): Future[String] = {
    if (status != StatusCodes.OK)
      logger.warn(s"Bad response status code: $status")
    entity.toStrict(timeout.seconds).map(_.data.utf8String)
  }

  def unmarshalBody[A](apiResponseBody: String, path: String)(
      implicit um: Unmarshaller[String, List[Either[ErrorMessageJson, A]]]
  ): Future[List[Either[ErrorMessageJson, A]]] =
    Unmarshal(apiResponseBody)
      .to[List[Either[ErrorMessageJson, A]]]
      .map(list => addPathToLeft(list, path))
      .recover {
        case ex =>
          List(Left(ErrorMessageJson(path, ex.getMessage, apiResponseBody, Instant.now)))
      }

  private def addPathToLeft[A](
      list: List[Either[ErrorMessageJson, A]],
      path: String
  ): List[Either[ErrorMessageJson, A]] =
    list.map(_.left.map(_.copy(errorSource = path)))
}
