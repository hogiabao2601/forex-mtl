package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.services.caches.CacheService
import forex.services.keys.KeyBuilder
import forex.services.oneframe.OneFrameService
import forex.services.rates.interpreters._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Sync](logger: SelfAwareStructuredLogger[F],
                       oneFrameService: OneFrameService[F],
                       cacheService: CacheService[F],
                       keyBuilder: KeyBuilder): Algebra[F] =
    OneFrameLive[F](logger, oneFrameService, cacheService, keyBuilder)
}
