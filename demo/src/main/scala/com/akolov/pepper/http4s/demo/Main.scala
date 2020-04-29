package com.akolov.pepper.http4s.demo

import cats.data.Kleisli
import cats.effect.ExitCode
import cats.implicits._
import com.akolov.pepper.http4s.ProtectedRoutes
import com.akolov.pepper.http4s.demo.AppType.AppTask
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._

import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.interop.catz._
import sttp.tapir.endpoint
import com.akolov.pepper.auth.RuleSyntax._

object AppType {
  type AppTask[A] = ZIO[zio.ZEnv, Throwable, A]
}



object Main
    extends zio.App with ProtectedRoutes[AppTask, DemoRuleEvaluator, List[sttp.model.Header], ErrorInfo]
    with DemoRules[AppTask] {

  val orgService = new OrganisationService[AppTask] {
    override def userAuthorized(child: String, parent: String): AppTask[Boolean] = ZIO.succeed(parent.contains(child))
  }
  implicit val logicLiftParams = LogicLiftParams(DemoRuleEvaluator(orgService), Forbidden, Unauthorized)
  implicit val endpointLiftParams = EndpointLiftParams(endpoint.input.and(sttp.tapir.headers))

  val routes: HttpRoutes[AppTask] = StatusRoute.statusEndpoint
    .toProtectedRoutes(StatusRoute.logic, hasRole("Admin") || (hasRole("User") && isMemberOfOrganisation {
      case s => s.toString
    }))

  def build(): ZIO[zio.ZEnv, Throwable, Unit] =
    ZIO.runtime[zio.ZEnv].flatMap { implicit rts =>
      val yaml = StatusRoute.statusEndpoint.toOpenAPI("Demo Pepper App", "1.0").toYaml

      val httpApp =
        Router("/" -> routes, "/docs" -> new SwaggerHttp4s(yaml).routes[AppTask]).orNotFound

      val finalHttpApp = Logger.httpApp[AppTask](true, true)(httpApp)

      BlazeServerBuilder[ZIO[zio.ZEnv, Throwable, *]]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
        .compile[ZIO[zio.ZEnv, Throwable, *], ZIO[zio.ZEnv, Throwable, *], ExitCode]
        .drain
    }

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    build().foldM(
      failure = (err: Throwable) => ZIO.effectTotal(err.printStackTrace()) *> ZIO.succeed(1),
      success = _ => ZIO.succeed(0))
}
