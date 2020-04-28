package com.akolov.doorman

import com.akolov.pepper.http4s.ProtectedRoutes
import com.akolov.pepper.http4s.demo.Main.AppTask
import com.akolov.pepper.http4s.demo.{
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
    extends Specification with ProtectedRoutes[AppTask, DemoRuleEvaluator, List[sttp.model.Header], ErrorInfo] {
  "The protected status route" should {
    "return 403" in new ProtectedTestContext {
      serve(Request[AppTask](Method.GET, uri"/status")).status must beEqualTo(Status.Unauthorized)
    }
  }
}

class ProtectedTestContext
    extends Scope with DemoRules[AppTask]
    with ProtectedRoutes[AppTask, DemoRuleEvaluator, List[sttp.model.Header], ErrorInfo] {

  val orgService = new OrganisationService[AppTask] {
    override def isChild(child: String, parent: String): AppTask[Boolean] = ZIO.succeed(parent.contains(child))
  }
  implicit val logicLiftParams = LogicLiftParams(DemoRuleEvaluator(orgService), Forbidden, Unauthorized)
  implicit val endpointLiftParams = EndpointLiftParams(endpoint.input.and(sttp.tapir.headers))

  val routes = Router("/" -> StatusRoute.statusEndpoint.toProtectedRoutes(StatusRoute.logic, hasRole("Admin"))).orNotFound

  def serve(request: Request[AppTask]): Response[AppTask] =
    zio.Runtime.default.unsafeRun(routes(request))
}
