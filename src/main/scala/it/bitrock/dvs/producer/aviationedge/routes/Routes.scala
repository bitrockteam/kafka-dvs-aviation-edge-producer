package it.bitrock.dvs.producer.aviationedge.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{get, path}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import it.bitrock.dvs.producer.aviationedge.config.ServerConfig

class Routes(serverConfig: ServerConfig) {
  val routes: Route = health

  private def health: Route = get {
    path(serverConfig.rest.healthPath) {
      complete(StatusCodes.OK)
    }
  }
}
