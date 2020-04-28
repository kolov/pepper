# Pepper

Authorisation for `tapir` endpoints.

Pepper allows to add rule-based authorisation on endpoints, without any changes to the endpoint 
description and logic.

# Usage

Read how to use `pepper` or jump right to the [demo](#demo)

Add dependency `"com.akolov" %% "pepper" % "0.0.1-SNAPSHOT"`.

### Autorisation input

Imagine, we want to make soem resource, e.g. organisation stauts at `/status/:orgId` available only to users that are
members of the organisation. The user Id is availble in the `X-Acme-User-Id` header. To do this we need:
 - (part of) the endpoint input, eg. the `orgId` path segment 
 - elements of the request which are not needed by the endpoint - the `X-Acme-User` header 
 - a rule that, given the input above, can determine if the user is authorised, possily using external service.

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
The argument of `belongsToOrganisation` is a partial function that retrieves th user from the endpoint input - int this case: String.

Some definitions:
```scala 

sealed trait AuthorizationResult
case object ForbiddenAccess extends AuthorizationResult
case object UnauthorizedAccess extends AuthorizationResult
case object AuthorizedAccess extends AuthorizationResult
```

To execute rules, we need so rule evaluation service - `RE[F]`, which will difer per application.  With it we define
```scala

/*
  F - the effect
  I - the endpoint input
  RE - Rule evaluation service
*/
case class Rule[F[_]: Monad, -I, RE[_[_]]](run: ((I, RE[F])) => F[AuthorizationResult])
```

To build an instance of `RE`, we need: 
## Developer's notes

    sbt '+ publishSigned'
    sbt sonatypeReleaseAll

    sbt '++2.12.10! docs/mdoc' // project-docs/target/mdoc/README.md
 


 


