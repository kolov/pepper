package com.akolov.pepper.http4s

import cats.Monad
import cats.effect.{ContextShift, Sync}
import com.akolov.pepper.auth.{LiftLogic, Lifting, Rule}
import org.http4s.HttpRoutes
import pepper.auth.LiftEndpoint
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{EndpointToHttp4sServer, Http4sServerOptions}

trait ProtectedRoutes[F[_], RE[_[_]], IA, E] extends LiftLogic[F, RE, IA, E] with LiftEndpoint[F, RE, IA, E] {

  implicit class ProtectedRoutesSyntax0[O](endpoint: Endpoint[Unit, E, O, Nothing]) {

    def toProtectedRoutes(
      logic: Unit => F[Either[E, O]],
      rule: Rule[F, RE]
    )(
      implicit logicLiftParams: LogicLiftParams,
      endpointLiftParams: EndpointLiftParams,
      sync: Sync[F],
      cs: ContextShift[F],
      opts: Http4sServerOptions[F]): HttpRoutes[F] = {
      val elo: Endpoint[IA, E, O, Nothing] = liftEndpoint0(endpoint)
      val xx: ServerEndpoint[IA, E, O, Nothing, F] =
        elo.serverLogic(withAuthRule0(logic, rule))
      new EndpointToHttp4sServer[F](opts).toRoutes(xx)
    }
  }

//  implicit class ProtectedRoutesSyntax1[I1, E, O](endpoint: Endpoint[I1, E, O, Nothing]) {
//
//    def toProtectedRoutes(
//      logic: I1 => F[Either[E, O]],
//      rule: Rule[F]
//    )(
//      factory: EvaluatorBuilder
//    ): HttpRoutes[F] = {
//      val elo: ServerEndpoint[(I1, PlainHeaders), E, (O, PlainHeaders), Nothing, F] =
//        endpoint
//          .copy(input = endpoint.input.and(headers), output = endpoint.output.and(headers))
//          .serverLogic[F](withAuthRule1(logic, rule))
//      new EndpointToHttp4sServer[F](opts).toRoutes(elo)
//    }
//  }
//
//  implicit class ProtectedRoutesSyntax2[I1, I2, E, O](endpoint: Endpoint[(I1, I2), ErroErInfo, O, Nothing]) {
//
//    def toProtectedRoutes(
//      logic: ((I1, I2)) => F[Either[E, O]],
//      rule: Rule[F]
//    )(
//      factory: EvaluatorBuilder
//    ): HttpRoutes[F] = {
//      val elo: ServerEndpoint[(I1, I2, PlainHeaders), E, (O, PlainHeaders), Nothing, F] =
//        endpoint
//          .copy(input = endpoint.input.and(headers), output = endpoint.output.and(headers))
//          .serverLogic[F](withAuthRule2(logic, rule))
//      new EndpointToHttp4sServer[F](opts).toRoutes(elo)
//    }
//  }
//
//  implicit class ProtectedRoutesSyntax3[I1, I2, I3, O](endpoint: Endpoint[(I1, I2, I3), E, O, Nothing]) {
//
//    def toProtectedRoutes(
//      logic: ((I1, I2, I3)) => F[Either[E, O]],
//      rule: Rule[F]
//    )(
//      factory: EvaluatorBuilder
//    ): HttpRoutes[F] = {
//      val elo: ServerEndpoint[(I1, I2, I3, PlainHeaders), E, (O, PlainHeaders), Nothing, F] =
//        endpoint
//          .copy(input = endpoint.input.and(headers), output = endpoint.output.and(headers))
//          .serverLogic[F](withAuthRule3(logic, rule))
//      new EndpointToHttp4sServer[F](opts).toRoutes(elo)
//    }
//  }
}
