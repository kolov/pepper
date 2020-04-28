package pepper.auth

import com.akolov.pepper.auth.{Lifting, Rule}
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint

trait LiftEndpoint[F[_], RE[_[_]], IA, E] extends Lifting[F, RE, IA, E] {

  def liftEndpoint0[O](
    endpoint: Endpoint[Unit, E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[IA, E, O, Nothing] = {
    val h: Endpoint[IA, E, O, Nothing] = endpoint.copy(input = endpoint.input.and(config.ias))
    h
  }

  def liftEndpoint1[I1, O](
    endpoint: Endpoint[I1, E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[(I1, IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }
}
