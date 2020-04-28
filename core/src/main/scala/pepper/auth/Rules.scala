package com.akolov.pepper.auth

import cats._, cats.implicits._

sealed trait AuthorizationResult extends Product with Serializable
case object ForbiddenAccess extends AuthorizationResult
case object UnauthorizedAccess extends AuthorizationResult
case object AuthorizedAccess extends AuthorizationResult

trait AuthorizationResultSyntax {

  implicit class syntax(outcome: AuthorizationResult) {

    def isSuccess: Boolean = outcome match {
      case AuthorizedAccess => true
      case _ => false
    }
  }

   
}
object AuthorizationResultSyntax extends AuthorizationResultSyntax

case class Rule[F[_]: Monad, RE[_[_]]](run: RE[F] => F[AuthorizationResult])

object Rule extends AuthorizationResultSyntax {

  def and[F[_]: Monad, RE[_[_]]](r1: Rule[F, RE], r2: Rule[F, RE]): Rule[F, RE] = Rule { svc =>
    for {
      outcome1 <- r1.run(svc)
      result <- if (outcome1.isSuccess) {
        r2.run(svc)
      } else
        Monad[F].pure(outcome1)
    } yield result
  }

  def or[F[_]: Monad, RE[_[_]]](r1: Rule[F, RE], r2: Rule[F, RE]): Rule[F, RE] = Rule { svc =>
    for {
      outcome1 <- r1.run(svc)
      result <- if (outcome1.isSuccess) {
        Monad[F].pure(outcome1)
      } else
        r2.run(svc)
    } yield result
  }
}

trait RuleSyntax {
  type PlainHeaders = Seq[(String, String)]

  implicit class RuleSyntax[F[_]: Monad, RE[_[_]]](rule: Rule[F, RE]) {
    def &&(other: Rule[F, RE]): Rule[F, RE] = Rule.and(rule, other)
    def ||(other: Rule[F, RE]): Rule[F, RE] = Rule.or(rule, other)
  }
}
object RuleSyntax extends RuleSyntax