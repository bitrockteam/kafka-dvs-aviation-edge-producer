package it.bitrock.dvs.producer.services

import java.util.Calendar

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.dvs.producer.model.{MessageJson, Tick}
import JsonSupport._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class AviationFlow()(implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext) extends LazyLogging {

  def flow(uri: Uri, apiTimeout: Int): Flow[Tick, List[MessageJson], NotUsed] = flow { () =>
    // Only for development purpose (2 hours shift: 4 -> 6 and 16 -> 18)
    val now          = Calendar.getInstance()
    val currentHour  = now.get(Calendar.HOUR_OF_DAY)
    val isWeekendDay = List(Calendar.SATURDAY, Calendar.SUNDAY).contains(now.get(Calendar.DAY_OF_WEEK))

    if (isWeekendDay || currentHour < 4 || currentHour > 16)
      Future("")
    else {
      //------------------------------------------------

      logger.info(s"Trying to call: $uri")
      Http().singleRequest(HttpRequest(HttpMethods.GET, uri)).flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          entity
            .toStrict(apiTimeout.seconds)
            .map(_.data.utf8String)
        case HttpResponse(statusCodes, _, _, _) =>
          logger.warn(s"Bad response status code: $statusCodes")
          Future("")
      }
    }
  }

  def flow(apiProvider: () => Future[String]): Flow[Tick, List[MessageJson], NotUsed] = {
    Flow
      .fromFunction((x: Tick) => x)
      .mapAsync(1) { _ =>
        apiProvider().flatMap(response => unmarshal(response))
      }
  }

  private def unmarshal(apiResponseBody: String): Future[List[MessageJson]] = {
    Unmarshal(apiResponseBody).to[List[MessageJson]].recover {
      case e =>
        logger.warn(s"Unmarshal error: $e")
        List[MessageJson]()
    }
  }
}
