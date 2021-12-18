package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.config.OneFrameConfig
import forex.services.caches.CacheService
import interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Sync](oneFrameConfig: OneFrameConfig,
                       httpClient: Client[F],
                       cacheService: CacheService[F]): Algebra[F] =
    OneFrameLive[F](oneFrameConfig, httpClient, cacheService)
}
