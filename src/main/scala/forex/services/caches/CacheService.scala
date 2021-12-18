package forex.services.caches

import forex.services.rates.RateResponse

trait CacheService[F[_]] {
  def getRate(key: String): F[Option[RateResponse]]
  def putRate(key: String, response: RateResponse): F[RateResponse]
}
