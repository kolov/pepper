import cats._, cats.implicits._

/**
  * Result with Proof. Prof is a List of headers to send back with the response declaring which rule allowed the authorisation.
  * THis is very specific case, but it happens to be need in the service I am working on.
  *
  * Most users can safely ignore this.
  *
  * @tparam P
  */
sealed trait AuthResultP[+P] extends Product with Serializable
case object ForbiddenAccessP extends AuthResultP[Any]
case object UnauthorizedAccessP extends AuthResultP[Any]
case class AuthorizedAccessP[P](proof: P) extends AuthResultP[P]

trait AuthorizationResultSyntax {

  implicit class syntax[P](outcome: AuthResultP[P]) {

    def isSuccess: Boolean = outcome match {
      case AuthorizedAccessP(_) => true
      case _ => false
    }
  }

  implicit def semigroupAuthorizationResult[P: Semigroup] = new Semigroup[AuthResultP[P]] {

    def combine(x: AuthResultP[P], y: AuthResultP[P]): AuthResultP[P] =
      (x, y) match {
        case (AuthorizedAccessP(h1), AuthorizedAccessP(h2)) => AuthorizedAccessP(h1 |+| h2)
        case (AuthorizedAccessP(_), denied) => denied
        case (denied, _) => denied
      }
  }
}
object AuthorizationResultSyntax extends AuthorizationResultSyntax

case class Rule[F[_]: Monad, RE[_[_]], P: Semigroup](run: RE[F] => F[AuthResultP[P]])

object Rule extends AuthorizationResultSyntax {

  def and[F[_]: Monad, RE[_[_]], P: Semigroup](r1: Rule[F, RE, P], r2: Rule[F, RE, P]): Rule[F, RE, P] = Rule { svc =>
    for {
      outcome1 <- r1.run(svc)
      result <- if (outcome1.isSuccess) {
        r2.run(svc).map(_ |+| outcome1)
      } else
        Monad[F].pure(outcome1)
    } yield result
  }

  def or[F[_]: Monad, RE[_[_]], P: Semigroup](r1: Rule[F, RE, P], r2: Rule[F, RE, P]): Rule[F, RE, P] = Rule { svc =>
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

  implicit class RuleSyntax[F[_]: Monad, RE[_[_]], P: Semigroup](rule: Rule[F, RE, P]) {
    def &&(other: Rule[F, RE, P]): Rule[F, RE, P] = Rule.and(rule, other)
    def ||(other: Rule[F, RE, P]): Rule[F, RE, P] = Rule.or(rule, other)
  }
}
object RuleSyntax extends RuleSyntax
