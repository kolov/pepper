package com.akolov.pepper.auth

import cats._, cats.implicits._

object AuthHeaders {
  val TnmUserHeader = "X-User-Id"
  val TnmRoleHeader = "X-User-Roles"
  val TnmOrganisationHeader = "X-Organisation-Id"
}

trait RuleMethods[F[_]] extends RuleSyntax {

  def hasRole(role: String)(implicit m: Monad[F]): Rule[F, RuleEvaluator] = Rule { svc =>
    val result: AuthorizationResult = if (svc.hasRole(role)) {
      AuthorizedAccess
    } else if (svc.hasAnyRole) {
      ForbiddenAccess
    } else {
      UnauthorizedAccess
    }
    Monad[F].pure(result)
  }

  def organisationHeaderIsParentOfParameter(ix: Int)(implicit m: Monad[F]): Rule[F, RuleEvaluator] = Rule { svc =>
    (svc.organisationHeader, svc.parameter(ix)) match {
      case (Some(headerOrg), Some(paramOrg)) =>
        val resultSuccess = AuthorizedAccess
        if (headerOrg == paramOrg)
          Monad[F].pure(resultSuccess)
        else
          svc.isChild(paramOrg, headerOrg).map { isChild =>
            if (isChild)
              resultSuccess
            else
              ForbiddenAccess
          }
      case (None, _) => Monad[F].pure(UnauthorizedAccess)
      case _ => Monad[F].pure(ForbiddenAccess)
    }
  }
}
