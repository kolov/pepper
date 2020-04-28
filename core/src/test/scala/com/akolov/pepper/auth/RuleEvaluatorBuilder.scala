package com.akolov.pepper.auth

trait RuleEvaluator[F[_]] {
  def hasAnyRole: Boolean
  def hasRole(role: String): Boolean
  val organisationHeader: Option[String]
  def parameter(ix: Int): Option[String]
  def isChild(child: String, parent: String): F[Boolean]
}

trait OrganisationService[F[_]] {
  def isChild(child: String, parent: String): F[Boolean]
}

object RuleEvaluator {

  def apply[F[_]](orgService: OrganisationService[F]): ((Seq[(String, String)], Seq[Any])) => RuleEvaluator[F] = {
    case (headers, endpointParameters) =>
      new RuleEvaluator[F] {

        lazy val roles: List[String] = headers
          .find(_._1 == AuthHeaders.TnmRoleHeader)
          .map(_._2.split(",").toList)
          .getOrElse(List.empty)

        override def hasRole(role: String): Boolean = roles.contains(role)

        override lazy val organisationHeader: Option[String] = headers
          .find(_._1 == AuthHeaders.TnmOrganisationHeader)
          .map(_._2)

        override def parameter(ix: Int): Option[String] =
          endpointParameters.lift(ix).map(_.toString)

        override def hasAnyRole: Boolean = roles.nonEmpty

        override def isChild(child: String, parent: String): F[Boolean] = {
          orgService.isChild(child, parent)
        }
      }
  }
}
