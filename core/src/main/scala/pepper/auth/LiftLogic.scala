package com.akolov.pepper.auth

import cats.Monad
import cats.implicits._
import sttp.tapir.EndpointIO.Basic
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._

trait Lifting[F[_], RE[_[_]], IA, E] {
  type EvaluatorBuilder = IA => RE[F] // RuleEvaluator

  case class LogicLiftParams(eb: EvaluatorBuilder, forbidden: E, unauthorized: E)
  case class EndpointLiftParams(ias: EndpointInput[IA])
}

// II: I + additional
trait LiftLogic[F[_], RE[_[_]], IA, E] extends Lifting[F, RE, IA, E] {

  def execute[I, O](
    rule: Rule[F, I, RE],
    i: I,
    ia: IA,
    onAllowed: => F[Either[E, O]]
  )(implicit config: LogicLiftParams, m: Monad[F]): F[Either[E, O]] = {
    for {
      auth <- rule.run((i, config.eb(ia)))
      r <- auth match {
        case AuthorizedAccess =>
          onAllowed
        case ForbiddenAccess =>
          Monad[F].pure(config.forbidden.asLeft)
        case UnauthorizedAccess =>
          Monad[F].pure(config.unauthorized.asLeft)
      }
    } yield r
  }

  /**
    * Converts function `Logic` to another function that:
    * * has the additional input parameter [[PlainHeaders]]
    * * uses these headers to execute the given rule
    * * invokes `logic` if the rule check passes
    * * returns a Forbidden if the rule fails
    */
  def withAuthRule0[O](
    logic: Unit => F[Either[E, O]],
    rule: Rule[F, Unit, RE]
  )(implicit config: LogicLiftParams, m: Monad[F]): IA => F[Either[E, O]] = { ia =>
    execute(rule, (), ia, logic(()))
  }

  def withAuthRule1[I1, O](
    logic: I1 => F[Either[E, O]],
    rule: Rule[F, I1, RE]
  )(implicit config: LogicLiftParams, m: Monad[F]): ((I1, IA)) => F[Either[E, O]] =
    (t: (I1, IA)) => execute(rule, t._1, t._2, logic(t._1))

  def withAuthRule2[I1, I2, O](
    logic: ((I1, I2)) => F[Either[E, O]],
    rule: Rule[F, (I1, I2), RE]
  )(implicit config: LogicLiftParams, m: Monad[F]): ((I1, I2, IA)) => F[Either[E, O]] =
    (t: (I1, I2, IA)) => execute(rule, (t._1, t._2), t._3, logic((t._1, t._2)))

  def withAuthRule3[I1, I2, I3, O](
    logic: ((I1, I2, I3)) => F[Either[E, O]],
    rule: Rule[F, (I1, I2, I3), RE]
  )(implicit config: LogicLiftParams, m: Monad[F]): ((I1, I2, I3, IA)) => F[Either[E, O]] =
    (t: (I1, I2, I3, IA)) => execute(rule, (t._1, t._2, t._3), t._4, logic((t._1, t._2, t._3)))
}
