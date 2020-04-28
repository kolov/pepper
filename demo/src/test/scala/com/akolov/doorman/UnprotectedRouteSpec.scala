package com.akolov.doorman

import com.akolov.pepper.http4s.demo.Main.AppTask
import com.akolov.pepper.http4s.demo.StatusRoute
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import sttp.tapir.server.http4s.Http4sServerOptions._
import sttp.tapir.server.http4s._
import zio.interop.catz._

class UnprotectedRouteSpec extends Specification{
  "The unprotected status route" should {
    "return OK" in new TestContext {
      serve(Request[AppTask](Method.GET, uri"/status")).status must beEqualTo(Status.Ok)
    }
  }

  class TestContext extends Scope {
    val routes = Router("/" -> StatusRoute.statusEndpoint.toRoutes(StatusRoute.logic)).orNotFound

    def serve(request: Request[AppTask]): Response[AppTask] =
      zio.Runtime.default.unsafeRun(routes(request))
  }
}
