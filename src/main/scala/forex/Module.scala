package forex

import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, Timer }
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.schedulers.jobs.OneFrameFetcher
import forex.schedulers.{ BaseJob, Scheduler }
import forex.services._
import forex.services.caches.interpreters.RedisService
import forex.services.keys.KeyBuilderOps
import forex.services.oneframe.interpreters.OneFrameServiceImpl
import forex.services.rates.interpreters.OneFrameLive
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger.getLoggerFromClass
import org.http4s._
import org.http4s.client.{ Client, _ }
import org.http4s.server.middleware.{ AutoSlash, Timeout }
import org.http4s.syntax.all._
import redis.clients.jedis.JedisPool

import scala.concurrent.ExecutionContext

class Module[F[_]: ConcurrentEffect: Timer: ContextShift](config: ApplicationConfig, ec: ExecutionContext) {

  import java.util.concurrent._

  val blockingPool          = Executors.newFixedThreadPool(5)
  val blocker               = Blocker.liftExecutionContext(ec)
  val httpClient: Client[F] = JavaNetClientBuilder[F](blocker).create

  val oneFrameService = {
    val logger = getLoggerFromClass(classOf[OneFrameServiceImpl[F]])
    new OneFrameServiceImpl(logger, config.oneFrame, httpClient)
  }

//  val memcachedClient = new MemcachedClient(
//    new BinaryConnectionFactory(),
//    AddrUtil.getAddresses(config.cache.url)
//  )
//  val cacheService = new MemcachedService(memcachedClient, config.cache)

  val jedisPool    = new JedisPool(config.cache.host, config.cache.port)
  val cacheService = new RedisService(jedisPool, config.cache)

  private val ratesService: RatesService[F] = {
    val logger = getLoggerFromClass(classOf[OneFrameLive[F]])
    RatesServices.live[F](logger, oneFrameService, cacheService, KeyBuilderOps)
  }

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

  val jobs: List[BaseJob[F]] = List(
    {
      val logger = getLoggerFromClass(classOf[OneFrameFetcher[F]])
      OneFrameFetcher(logger, config.scheduler.oneFrameFetcher, oneFrameService, cacheService, KeyBuilderOps)
    }
  )
  val scheduler = new Scheduler[F](jobs)

}
