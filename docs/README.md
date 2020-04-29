# Pepper

Authorisation for `tapir` endpoints.

Add rule-based authorisation to existing authorisation-agnostic endpoint, without any changes to
the endpoint description and logic.


## Teaser

If you don't know tapir - you should have a look. It is the best way to build REST services in Scala. In short,
first you define an endpoint - a description of what parameters the endpoint takes. An endpoint
serving `/status/:orgId?fields=name,active` gets 2 parameters from the request, and has type
`Endpoint[(String, String), ErrorInfo, Status, Nothing]` , assuming it returns a value of type `Status`.
Note that and endpoint is just a value. If you couple it with a logic with a fitting signature, Tapir can 
compute a http4s or akka-http route:

```scala
val statusEndpoint: Endpoint[(String, String), ErrorInfo, Status, Nothing]
val statusLogic: (String, String) => F[Either[E,O]]
val route = statusEndpoint.toRoutes(statusLogic)
```

Pepper allows you to transparently add authorisation logic to the endpoint. The application has 
to provide its own type whose functions are the building blocks for the rules, 
  and combine them in rules that are needed by the application:

```scala
val rule = hasRole("Admin") || (hasRole("User") && isMemberOfOrganisation { case (orgId, _) => orgId })
val protectedRoute = statusEndpoint.toProtectedRoutes(statusLogic, rule)
```

Pepper will lift `statusLogic` to first evaluate the rule and either return Forbidden/Unauthorized, 
or run the logic and return the result. `hasRole`, `isMemberOfOrganisation` etc. are not part
of Pepper - they are defined and provided by the application, represented by tye `RE`. 
Pepper will also lift the endpoint
to collect additional parameters from the request (e.g. headers) needed
to buiild an instans of `RE`:

```scala 
 Endpoint[I, E, O, S]   =>   Endpoint[(I, IA), E, O, S]```
 I => F[Either[E,O]]    =>   (I, IA) => F[Either[E,O]]
```

The additional parameters ar of type `IA`, described with `EndpointInput[IA]`.

This may sound complicated. The documentation is in early stage, and 
the explaining clearly is hard.
Jump to the Demo application and see the sample code below, or to the [Demo](#demo).
    
## Authorisation input data

In the example above, we want to make `/status/:orgId` accessible only by users that are
members of the organisation. The User Id is available in the `X-Acme-User-Id` header. To do this we need:
 - (part of) the endpoint input, - the `orgId` path segment.
 - elements of the request which are not needed by the endpoint - the `X-Acme-User-Id` header. We'll call this type `IA`: `Header` or `List[Header]`, in this example.
 - a rule that, given both pieces of data, can determine if the user is authorized. 
 
A Rule us defined as

```scala
/*
  F - the effect
  I - the endpoint input
  RE -  Rule Evaluation type

*/
case class Rule[F[_]: Monad, -I, RE[_[_]]](run: ((I, RE[F])) => F[AuthorizationResult])
```
An instance of `RE[F]`, can be built from a value of type `IA` - (List[Header]).

So this request
```bash curl 
GET /organisation/1232321?fields=name,active 

X-User-Id: 0559fffa-ff00-4472-889b-a55d1ad1757f
X-Role: User
```

can be protected with the following rule in `Pepper`:
``` scala
  hasRole("Admin") || (hasRole("User") && isMemberOfOrganisation { case (orgId, _) =< orgId}
``` 

To do this, we need Rules:

```scala
def hasRole(role: String): Rule[F, Any, DemoRuleEvaluator] = ???
def isMemberOfOrganisation(f: PartialFunction[Any, String]): Rule[F, String, DemoRuleEvaluator] = = ???
```

The application has to define a type allowing the implementation of those rules, e.g.:
```scala
trait DemoRuleEvaluator[F[_]] {
 def hasAnyRole: Boolean
  def hasRole(role: String): Boolean
  val userFromHeader: Option[String]
  def userAuthorized(userId: String, orgId: String): F[Boolean]
}                                                                                                                                                                                      }
```
and a way to build an instance of this trait: `ListpHeader] => DemoRuleEvaluator[Task]`. An example follows below. 

To lift the endpoint and the logic to:
 - `endpoint : Endpoint[(I, IA), E, O, S]`
 - `logic: (I, IA) => F[Either[E, O]]` 
 
Pepper needs a few implicits, packed in two case classes:
```scala
trait Lifting[F[_], RE[_[_]], IA, E] {
  type EvaluatorBuilder = IA => RE[F] // RuleEvaluator

  case class LogicLiftParams(eb: EvaluatorBuilder, forbiddenValue: E, unauthorizedValue: E)
  case class EndpointLiftParams(ias: EndpointInput[IA])
}
```

Thjese are all building block, see the example.

## Example

```scala

// The application rules logic needs theis building blocks
trait DemoRuleEvaluator[F[_]] {
  def hasAnyRole: Boolean
  def hasRole(role: String): Boolean
  val userFromHeader: Option[String]
  def userAuthorized(userId: String, orgId: String): F[Boolean]
}

// The actual ceck is an effect, possibly a database lookup
trait OrganisationService[F[_]] {
  def userAuthorized(userId: String, orgId: String): F[Boolean]
}

// We can use the functions from DemoRuleEvaluator to define rules:
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

  def isMemberOfOrganisation(f: PartialFunction[Any, String])(implicit m: Monad[F]): Rule[F, String, DemoRuleEvaluator] =
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

    // return IA => RE[F] 
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

// All is in place, define the endpoint and the server logic as usual

val statusEndpoint: Endpoint[String, ErrorInfo, String, Nothing] = endpoint.get
    .summary("Organisation status")
    .description("returns 200 if organisation status is OK")
    .in("status" / path[String]("id"))
    .out(plainBody[String])
    .outError( ...)


  val logic: String => AppTask[Either[ErrorInfo, String]] = id => ZIO.succeed(s"Item $id is OK".asRight)

// build protected route
  val routes = statusEndpoint.toProtectedRoutes(logic, ) 
                   hasRole("Admin ") || (hasRole("User")  && isMemberOfOrganisation {
                        case s => s.toString
                      }))

```
Run the `ProtectedRouteSpec` or the [Demo](#demo) to see this in action.


## Demo 

The demo module runs a simple ZIO-based server, where the only endpoint `/status/:orgId` 
is protected with this rule:

```scala
val routes = StatusRoute.statusEndpoint
    .toProtectedRoutes(StatusRoute.logic, hasRole("Admin") || (hasRole("User") && isMemberOfOrganisation {
      case s => s.toString
    }))
```

The service determining if a user belongs to an organisation:
```scala
val orgService = new OrganisationService[AppTask] {
    override def userAuthorized(child: String, parent: String) = 
       ZIO.succeed(parent.contains(child))
  }
```

After `sbt demo/run`:

```
~/p/a/pepper> curl localhost:8080/status/100 -H"X-User-Roles: Admin" -i
HTTP/1.1 200 OK 
Item 100 is OK⏎ 
                                                                                                                                                                                 
~/p/a/pepper> curl localhost:8080/status/100 -H"X-User-Roles: User" -i
HTTP/1.1 403 Forbidden

~/p/a/pepper> curl localhost:8080/status/100 -H"X-User-Roles: User" -H"X-User-Id: 10" -i
HTTP/1.1 200 OK
Item 100 is OK⏎  
                                                                                                                                                                                
~/p/a/pepper> curl localhost:8080/status/100 -H"X-User-Roles: User" -H"X-User-Id: 11" -i
HTTP/1.1 403 Forbidden

```

# Dependencies

Nothing has been released yet. After release:

    `"com.akolov" %% "pepper-core" % "${version}"`. 
  
For http4s routes,

    `"com.akolov" %% "pepper-http4s" % "${version}T"`
    
    
## Developer's notes

    sbt publishSigned
    sbt sonatypeReleaseAll

    sbt '++2.12.10! docs/mdoc' // project-docs/target/mdoc/README.md    
 


 


