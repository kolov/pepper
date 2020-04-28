package com.akolov.pepper.http4s.demo

import cats._
import com.akolov.pepper.auth._

object AuthHeaders {
  val UserHeader = "X-User-Id"
  val RoleHeader = "X-User-Roles"
  val OrganisationHeader = "X-Organisation-Id"
}

trait DemoRules[F[_]] {

  def hasRole(role: String)(implicit m: Monad[F]): Rule[F, DemoRuleEvaluator] = Rule { svc =>
    val result: AuthorizationResult = if (svc.hasRole(role)) {
      AuthorizedAccess
    } else if (svc.hasAnyRole) {
      ForbiddenAccess
    } else {
      UnauthorizedAccess
    }
    Monad[F].pure(result)
  }
}