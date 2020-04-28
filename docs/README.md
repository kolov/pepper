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
 - Authorisation input `IA`
 - `rule: Rule[F, I, RE]` 
We can lift them to:
 - `endpoint : Endpoint[(I, IA), E, O, S]`
 - `logic: (I, IA) => F[Either[E, O]]` 
 
and build a new route with one function `toProtectedRoutes`, similar to `toRoutes`:
```endpoint.toProtectedRoutes(logic, rule)```

## Demo

Not implemented, see the `ProtectedRouteSpec` in the `demo` project.

## Developer's notes

    sbt '+ publishSigned'
    sbt sonatypeReleaseAll

    sbt '++2.12.10! docs/mdoc' // project-docs/target/mdoc/README.md
 


 


