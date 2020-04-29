package com.akolov.pepper

import cats.data.Kleisli
import com.akolov.pepper.auth.Rule
import com.akolov.pepper.http4s.ProtectedRoutes
import com.akolov.pepper.http4s.demo._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.specs2.mutable.Specification
import sttp.tapir.endpoint
import zio.ZIO
import zio.interop.catz._
import com.akolov.pepper.auth.RuleSyntax._
import com.akolov.pepper.http4s.demo.AppType.AppTask

class ProtectedRouteSpec
    extends Specification with ProtectedRoutes[AppTask, DemoRuleEvaluator, List[sttp.model.Header], ErrorInfo]
    with DemoRules[AppTask] {

  val orgService = new OrganisationService[AppTask] {
    override def userAuthorized(child: String, parent: String): AppTask[Boolean] = ZIO.succeed(parent.contains(child))
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

      val request =
        Request[AppTask](Method.GET, uri = uri"/status/1", headers = Headers.of(Header(AuthHeaders.RoleHeader, "User")))

      serve(routes, request).status must beEqualTo(Status.Forbidden)
    }

    "return 200 with correct role authorization" in {
      val routeAdmin = protectedWithRules(rule = hasRole("Admin"))

      val request = Request[AppTask](
        Method.GET,
        uri = uri"/status/1",
        headers = Headers.of(Header(AuthHeaders.RoleHeader, "Admin")))

      serve(routeAdmin, request).status must beEqualTo(Status.Ok)
    }

    "return 200 with any of both roles" in {
      val routeUserOrAdmin = protectedWithRules(rule = hasRole("Admin") || hasRole("User"))

      val requestUser =
        Request[AppTask](Method.GET, uri = uri"/status/1", headers = Headers.of(Header(AuthHeaders.RoleHeader, "User")))
      val requestAdmin = Request[AppTask](
        Method.GET,
        uri = uri"/status/1",
        headers = Headers.of(Header(AuthHeaders.RoleHeader, "Admin")))

      serve(routeUserOrAdmin, requestUser).status must beEqualTo(Status.Ok)
      serve(routeUserOrAdmin, requestAdmin).status must beEqualTo(Status.Ok)
    }

    "return 401 when User Not part of organisation" in {

      val routeUserOrAdmin = protectedWithRules(rule = hasRole("User") || isMemberOfOrganisation {
        case s => s.toString
      })

      val requestUser2 =
        Request[AppTask](Method.GET, uri = uri"/status/1", headers = Headers.of(Header(AuthHeaders.UserHeader, "2")))

      serve(routeUserOrAdmin, requestUser2).status must beEqualTo(Status.Forbidden)
    }

    "return 200 when User is part of organisation" in {
      val routeUserOrAdmin = protectedWithRules(rule = hasRole("User") || isMemberOfOrganisation {
        case s => s.toString
      })

      val requestUser2 =
        Request[AppTask](Method.GET, uri = uri"/status/1", headers = Headers.of(Header(AuthHeaders.UserHeader, "1")))

      serve(routeUserOrAdmin, requestUser2).status must beEqualTo(Status.Ok)
    }
  }

  def protectedWithRules(
    rule: Rule[AppTask, String, DemoRuleEvaluator]): Kleisli[AppTask, Request[AppTask], Response[AppTask]] =
    Router("/" -> StatusRoute.statusEndpoint.toProtectedRoutes(StatusRoute.logic, rule)).orNotFound
}
