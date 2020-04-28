package com.akolov.pepper.http4s.demo

import cats.implicits._
import com.akolov.pepper.http4s.demo.AppType.AppTask
import sttp.model.StatusCode
import sttp.tapir.Codec._
import sttp.tapir._
import zio.ZIO

trait ErrorInfo
case object Forbidden extends ErrorInfo
case object Unauthorized extends ErrorInfo
case object ServiceError extends ErrorInfo

object StatusRoute {
  System.currentTimeMillis()

  implicit def errorInfoMapping[E <: ErrorInfo](e: E): EndpointOutput[ErrorInfo] =
    emptyOutput.map(_ => e: ErrorInfo)(_ => ())

  val baseEndpoint = endpoint.errorOut(
    oneOf[ErrorInfo](
      statusMappingValueMatcher(StatusCode.InternalServerError, errorInfoMapping(ServiceError)) {
        case ServiceError => true
      },
      statusMappingValueMatcher(StatusCode.Unauthorized, errorInfoMapping(Unauthorized)) {
        case ServiceError => true
      },
      statusMappingValueMatcher(StatusCode.Forbidden, errorInfoMapping(Forbidden)) {
        case ServiceError => true
      }
    )
  )

  implicit class errorSyntax[I, O](e: Endpoint[I, Unit, O, Nothing]) {

    def baseError: Endpoint[I, ErrorInfo, O, Nothing] = e.errorOut(
      oneOf[ErrorInfo](
        statusMappingValueMatcher(StatusCode.InternalServerError, errorInfoMapping(ServiceError)) {
          case ServiceError => true
        },
        statusMappingValueMatcher(StatusCode.Unauthorized, errorInfoMapping(Unauthorized)) {
          case Unauthorized => true
        },
        statusMappingValueMatcher(StatusCode.Forbidden, errorInfoMapping(Forbidden)) {
          case Forbidden => true
        }
      )
    )
  }

  val statusEndpoint: Endpoint[String, ErrorInfo, String, Nothing] = endpoint.get
    .summary("Service health")
    .description("returns 200 if service operates normally.")
    .in("status" / sttp.tapir.path[String]("id"))
    .out(plainBody[String])
    .baseError

  val logic: String => AppTask[Either[ErrorInfo, String]] = id => ZIO.succeed(s"Item $id is OK".asRight)
}
