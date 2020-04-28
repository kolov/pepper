package com.akolov.pepper.http4s.demo

import cats._, cats.implicits._
import com.akolov.pepper.auth._

object AuthHeaders {
  val UserHeader = "X-User-Id"
  val RoleHeader = "X-User-Roles"
  val OrganisationHeader = "X-Organisation-Id"
}

trait DemoRules[F[_]] {

  def hasRole(role: String)(implicit m: Monad[F]): Rule[F, Any, DemoRuleEvaluator] = Rule {
    case (_, svc) =>
      val result: AuthorizationResult = if (svc.hasRole(role)) {
        AuthorizedAccess
      } else if (svc.hasAnyRole) {
        ForbiddenAccess
      } else {
        UnauthorizedAccess
      }
      Monad[F].pure(result)
  }

  def belongsToOrganisation(f: PartialFunction[Any, String])(implicit m: Monad[F]): Rule[F, String, DemoRuleEvaluator] =
    Rule {
      case (i, svc) =>
        if (f.isDefinedAt(i)) {
          val organisationPathSegment: String = f(i)
          svc.userFromHeader.map { userIdfromheader =>
            svc.userAuthorized(userIdfromheader, organisationPathSegment).map {
              case true => AuthorizedAccess
              case false => ForbiddenAccess
            }
          }.getOrElse(Monad[F].pure(ForbiddenAccess))
        } else {
          Monad[F].pure(UnauthorizedAccess)
        }
    }
}
