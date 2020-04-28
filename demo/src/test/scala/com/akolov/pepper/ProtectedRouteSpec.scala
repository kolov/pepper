package com.akolov.pepper

import cats.data.Kleisli
import com.akolov.pepper.auth.Rule
import com.akolov.pepper.http4s.ProtectedRoutes
import com.akolov.pepper.http4s.demo.Main.AppTask
import com.akolov.pepper.http4s.demo.{
  AuthHeaders,
  DemoRuleEvaluator,
  DemoRules,
  ErrorInfo,
  Forbidden,
  OrganisationService,
  StatusRoute,
  Unauthorized
}
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import sttp.tapir.endpoint
import zio.ZIO
import zio.interop.catz._

class ProtectedRouteSpec
    extends Specification with ProtectedRoutes[AppTask, DemoRuleEvaluator, List[sttp.model.Header], ErrorInfo]
    with DemoRules[AppTask] {

  val orgService = new OrganisationService[AppTask] {
    override def isChild(child: String, parent: String): AppTask[Boolean] = ZIO.succeed(parent.contains(child))
  }
  implicit val logicLiftParams = LogicLiftParams(DemoRuleEvaluator(orgService), Forbidden, Unauthorized)
  implicit val endpointLiftParams = EndpointLiftParams(endpoint.input.and(sttp.tapir.headers))

  def serve(
    routes: Kleisli[AppTask, Request[AppTask], Response[AppTask]],
    request: Request[AppTask]): Response[AppTask] =
    zio.Runtime.default.unsafeRun(routes(request))

  "The protected status route" should {

    "return 403 without role authorization" in {
      val routes = protectedWithRules(rule = hasRole("Admin"))
      serve(routes, Request[AppTask](Method.GET, uri"/status/1")).status must beEqualTo(Status.Unauthorized)
    }

    "return 401 without wrong role authorization" in {
      val routes = protectedWithRules(rule = hasRole("Admin"))

      val request = Request[AppTask](Method.GET,
        uri = uri"/status/1",
        headers = Headers.of(Header(AuthHeaders.RoleHeader, "User")))

      serve(routes, request).status must beEqualTo(Status.Forbidden)
    }

    "return 200 with correct role authorization" in {
      val routes = protectedWithRules(rule = hasRole("Admin"))

      val request = Request[AppTask](Method.GET,
        uri = uri"/status/1",
        headers = Headers.of(Header(AuthHeaders.RoleHeader, "Admin")))

      serve(routes, request).status must beEqualTo(Status.Ok)
    }
  }

  def protectedWithRules(
    rule: Rule[AppTask, DemoRuleEvaluator]): Kleisli[AppTask, Request[AppTask], Response[AppTask]] =
    Router("/" -> StatusRoute.statusEndpoint.toProtectedRoutes(StatusRoute.logic, rule)).orNotFound
}
