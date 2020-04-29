package pepper.auth

import com.akolov.pepper.auth.{Lifting, Rule}
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint

/*
 * Lift endpoints takin, 0, 1, 2, 3 etc input parameters
 */
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

  def liftEndpoint2[I1, I2, O](
    endpoint: Endpoint[(I1, I2), E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[((I1, I2), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }

  def liftEndpoint3[I1, I2, I3, O](
    endpoint: Endpoint[(I1, I2, I3), E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[((I1, I2, I3), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }

  def liftEndpoint4[I1, I2, I3, I4, O](
    endpoint: Endpoint[(I1, I2, I3, I4), E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[((I1, I2, I3, I4), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }

  def liftEndpoint5[I1, I2, I3, I4, I5, O](
    endpoint: Endpoint[(I1, I2, I3, I4, I5), E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[((I1, I2, I3, I4, I5), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }

  def liftEndpoint6[I1, I2, I3, I4, I5, I6, O](
    endpoint: Endpoint[(I1, I2, I3, I4, I5, I6), E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[((I1, I2, I3, I4, I5, I6), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }

  def liftEndpoint7[I1, I2, I3, I4, I5, I6, I7, O](
    endpoint: Endpoint[(I1, I2, I3, I4, I5, I6, I7), E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[((I1, I2, I3, I4, I5, I6, I7), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }

  def liftEndpoint8[I1, I2, I3, I4, I5, I6, I7, I8, O](
    endpoint: Endpoint[(I1, I2, I3, I4, I5, I6, I7, I8), E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[((I1, I2, I3, I4, I5, I6, I7, I8), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }

  def liftEndpoint9[I1, I2, I3, I4, I5, I6, I7, I8, I9, O](
    endpoint: Endpoint[(I1, I2, I3, I4, I5, I6, I7, I8, I9), E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[((I1, I2, I3, I4, I5, I6, I7, I8, I9), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }

  def liftEndpoint10[I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, O](
    endpoint: Endpoint[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10), E, O, Nothing]
  )(implicit config: EndpointLiftParams): Endpoint[((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }

  def liftEndpoint11[I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, O](
    endpoint: Endpoint[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11), E, O, Nothing]
  )(implicit config: EndpointLiftParams)
    : Endpoint[((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11), IA), E, O, Nothing] = {
    endpoint.copy(input = endpoint.input.and(config.ias))
  }
}
