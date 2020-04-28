# Pepper

Authorisation for `tapir` endpoints.

Pepper allows to add rule-based authorisation on endpoints, without any changes to the endpoint 
description and logic.

# Usage

Read how to use `pepper` or jump right to the [demo](#demo)

Add dependency `"com.akolov" %% "pepper" % "0.0.1-SNAPSHOT"`.

### Autorisation input

Authorizing a request is based on:
 - part of of the endpoint input, eg. a segment of the request path
 - other parts of the request which are not needed by the endpoint

For example, this request
```bash curl 
GET /organisation/1232321/accounts

X-User-Id: 0559fffa-ff00-4472-889b-a55d1ad1757f
X-Role: User
```

may have the following authorization rule:
``` scala
  hasRole("System") || (hasRole("User") &&  isPartOfOrganization(userId, segment("organisation"))
``` 
 
## Developer's notes

    sbt '+ publishSigned'
    sbt sonatypeReleaseAll

    sbt '++2.12.10! docs/mdoc' // project-docs/target/mdoc/README.md
 


 


