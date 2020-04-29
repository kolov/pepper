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

case class Rule[F[_]: Monad, -I, RE[_[_]]](run: ((I, RE[F])) => F[AuthorizationResult])

object Rule extends AuthorizationResultSyntax {

  def and[F[_]: Monad, I, RE[_[_]]](r1: Rule[F, I, RE], r2: Rule[F, I, RE]): Rule[F, I, RE] = Rule {
    case (i, svc) =>
      for {
        outcome1 <- r1.run((i, svc))
        result <- if (outcome1.isSuccess) {
          r2.run((i, svc))
        } else
          Monad[F].pure(outcome1)
      } yield result
  }

  def or[F[_]: Monad, I, RE[_[_]]](r1: Rule[F, I, RE], r2: Rule[F, I, RE]): Rule[F, I, RE] = Rule {
    case (i, svc) =>
      for {
        outcome1 <- r1.run((i, svc))
        result <- if (outcome1.isSuccess) {
          Monad[F].pure(outcome1)
        } else
          r2.run((i, svc))
      } yield result
  }
}

trait RuleSyntax {

  implicit class RuleSyntax[F[_]: Monad, I, RE[_[_]]](rule: Rule[F, I, RE]) {
    def &&[I2 <: I](other: Rule[F, I2, RE]): Rule[F, I2, RE] = Rule.and(rule, other)
    def ||[I2 <: I](other: Rule[F, I2, RE]): Rule[F, I2, RE] = Rule.or(rule, other)
  }
}
object RuleSyntax extends RuleSyntax
