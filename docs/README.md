# Pepper

Authorisation for `tapir` endpoints.

Pepper allows to add rule-based authorisation on endpoints, without any changes to the existing 
authorisation-agnostic endpoint description and logic.


# Usage

Add dependency `"com.akolov" %% "pepper" % "0.0.1-SNAPSHOT"`.

### Autorisation input

Imagine, we want to make some resource, e.g. organisation status at `/status/:orgId` available only to users that are
members of the organisation. The user Id is availble in the `X-Acme-User-Id` header. To do this we need:
 - (part of) the endpoint input, eg. the `orgId` path segment.
 - elements of the request which are not needed by the endpoint - the `X-Acme-User` header. We'll call this type (`List[Header]`, in this case), `IA`.
 - a rule that, given the data above, can determine if the user is authorised. To do do that, the rul will need som Rule evaluation service `RE[F]`:
```scala

/*
  F - the effect
  I - the endpoint input
  RE - Rule evaluation service
*/
case class Rule[F[_]: Monad, -I, RE[_[_]]](run: ((I, RE[F])) => F[AuthorizationResult])
```

For example, this request
```bash curl 
GET /organisation/1232321/accounts

X-User-Id: 0559fffa-ff00-4472-889b-a55d1ad1757f
X-Role: User
```

can be described with the following rule in `Pepper`:
``` scala
  hasRole("Admin") || (hasRole("User") && belongsToOrganisation { case s => s }
``` 
Both `hasRole` and `belongsToOrganisation` are methods of `RE`, defined by the application. 
The argument of `belongsToOrganisation` is a partial function that retrieves the user from the endpoint 
input - in this particular case, it is `PartionFunction[String, String]`.


With a few implicits in place, given: 
 - `endpoint : Endpoint[I, E, O, S]`
 - `logic: I => F[Either[E, O]]` 
 - `EndpointInpt[IA]`, allowing to retrieve `IA` from the request
 - `rule: Rule[F, I, RE]` 
We can lift the endpoint and the logic to:
 - `endpoint : Endpoint[(I, IA), E, O, S]`
 - `logic: (I, IA) => F[Either[E, O]]` 
 
and build a new `http4s` route with one function `toProtectedRoutes`, similar to `toRoutes`:
```endpoint.toProtectedRoutes(logic, rule)```

## Example

```scala
trait DemoRuleEvaluator[F[_]] {
  def hasAnyRole: Boolean
  def hasRole(role: String): Boolean
  val userFromHeader: Option[String]
  def userAuthorized(userId: String, orgId: String): F[Boolean]
}

trait OrganisationService[F[_]] {
  def userAuthorized(userId: String, orgId: String): F[Boolean]
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

object DemoRuleEvaluator {

    def apply[F[_]](orgService: OrganisationService[F]): List[Header] => DemoRuleEvaluator[F] = { headers =>
      new DemoRuleEvaluator[F] {
        lazy val roles: List[String] = headers
          .find(_.name == AuthHeaders.RoleHeader)
          .map(_.value.split(",").toList)
          .getOrElse(List.empty)
  
        override def hasRole(role: String): Boolean = roles.contains(role)
  
        override lazy val userFromHeader: Option[String] = headers
          .find(_.name == AuthHeaders.UserHeader)
          .map(_.value)
  
        override def hasAnyRole: Boolean = roles.nonEmpty
  
        override def userAuthorized(userId: String, orgId: String): F[Boolean] = {
          orgService.userAuthorized(userId, orgId)
        }
      }
    }
}

val statusEndpoint: Endpoint[String, ErrorInfo, String, Nothing] = endpoint.get
    .summary("Service status")
    .description("returns 200 if service operates normally.")
    .in("status" / path[String]("id"))
    .out(plainBody[String])
    .outError( ...)


  val logic: String => AppTask[Either[ErrorInfo, String]] = id => ZIO.succeed(s"Item $id is OK".asRight)

  val routes = statusEndpoint.toProtectedRoutes(logic, ) 
                   hasRole("Admin ") && (hasRole("User")  || belongsToOrganisation {
                        case s => s.toString
                      })

```
Run the tests to see this in action.
## Demo

Not implemented, see the `ProtectedRouteSpec` in the `demo` project.

## Developer's notes

    sbt '+ publishSigned'
    sbt sonatypeReleaseAll

    sbt '++2.12.10! docs/mdoc' // project-docs/target/mdoc/README.md    
 


 


