package com.akolov.pepper.http4s.demo

import sttp.model.Header

trait DemoRuleEvaluator[F[_]] {
  def hasAnyRole: Boolean
  def hasRole(role: String): Boolean
  val organisationHeader: Option[String]
  def isChild(child: String, parent: String): F[Boolean]
}

trait OrganisationService[F[_]] {
  def isChild(child: String, parent: String): F[Boolean]
}

object DemoRuleEvaluator {

  def apply[F[_]](orgService: OrganisationService[F]): List[Header] => DemoRuleEvaluator[F] = { headers =>
    new DemoRuleEvaluator[F] {
      lazy val roles: List[String] = headers
        .find(_.name == AuthHeaders.RoleHeader)
        .map(_.value.split(",").toList)
        .getOrElse(List.empty)

      override def hasRole(role: String): Boolean = roles.contains(role)

      override lazy val organisationHeader: Option[String] = headers
        .find(_.name == AuthHeaders.OrganisationHeader)
        .map(_.value)

      override def hasAnyRole: Boolean = roles.nonEmpty

      override def isChild(child: String, parent: String): F[Boolean] = {
        orgService.isChild(child, parent)
      }
    }
  }
}
