package forex.services.caches.interpreters

import cats.effect.{ Async => CatsAsync }
import cats.implicits.catsSyntaxOptionId
import cats.syntax.applicative._
import cats.syntax.apply._
import forex.services.caches.CacheService
import forex.services.rates.RateResponse
import forex.services.rates.RateResponse._
import redis.clients.jedis.JedisPool
import scalacache.redis.RedisCache
import scalacache.serialization.circe._
import scalacache.{ get, put, Cache, Mode }

import scala.concurrent.duration.DurationInt

class RedisService[F[_]: CatsAsync](pool: JedisPool, conf: forex.config.CacheConfig) extends CacheService[F] {
  implicit val mode: Mode[F] = scalacache.CatsEffect.modes.async[F]

  implicit val rateResponseCache: Cache[RateResponse] = RedisCache(pool)

  override def getRate(key: String): F[Option[RateResponse]] =
    if (conf.enable) {
      get(key)
    } else {
      (None: Option[RateResponse]).pure[F]
    }

  override def putRate(key: String, response: RateResponse): F[RateResponse] =
    if (conf.enable) {
      put(key)(response, conf.ttl.seconds.some) *>
        response.pure[F]
    } else {
      response.pure[F]
    }
}
