package forex.services.caches.interpreters

import cats.effect.{ Async => CatsAsync }
import cats.syntax.applicative._
import cats.syntax.apply._
import forex.services.caches.CacheService
import forex.services.rates.RateResponse
import forex.services.rates.RateResponse._
import net.spy.memcached.MemcachedClient
import scalacache.memcached.MemcachedCache
import scalacache.serialization.circe._
import scalacache.{ Cache, get, _ }

import scala.concurrent.duration.Duration

class MemcachedService[F[_]: CatsAsync](client: MemcachedClient, ttl: Option[Duration] = None) extends CacheService[F] {
  implicit val mode: Mode[F] = scalacache.CatsEffect.modes.async[F]

  override def getRate(key: String): F[Option[RateResponse]] = {
    implicit val rateResponseCache: Cache[RateResponse] = MemcachedCache[RateResponse](client)
    get(key)
  }

  override def putRate(key: String, response: RateResponse): F[RateResponse] = {
    implicit val rateResponseCache: Cache[RateResponse] = MemcachedCache[RateResponse](client)
    put(key)(response, ttl) *> response.pure[F]
  }
}
