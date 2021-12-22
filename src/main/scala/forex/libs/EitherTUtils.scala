package forex.libs

import cats.{ Applicative, Functor }
import cats.data.EitherT

object EitherTUtils {
  implicit class FToEitherT[F[_]: Functor, A](x: F[A]) {
    def toEitherT[E]: EitherT[F, E, A] = EitherT.right[E](x)
  }

  implicit class FEitherToEitherT[F[_]: Functor, E, A](x: F[E Either A]) {
    def toEitherT: EitherT[F, E, A] = EitherT(x)
  }

  implicit class EitherToEitherT[E, A](x: Either[E, A]) {
    def toEitherT[F[_]: Applicative]: EitherT[F, E, A] = EitherT.fromEither(x)
  }
}
