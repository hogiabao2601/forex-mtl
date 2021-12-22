package forex.schedulers.jobs

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cron4s.Cron
import cron4s.expr.CronExpr
import forex.config.OneFrameFetcherConfig
import forex.config.SchedulerConfig.TaskConfig
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.libs.EitherTUtils._
import forex.schedulers.BaseJob
import forex.services.caches.CacheService
import forex.services.keys.KeyBuilder
import forex.services.oneframe.OneFrameService
import forex.services.oneframe.OneFrameService.OneFrameServiceError
import forex.services.rates.RateResponse
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

import java.time.Instant

class OneFrameFetcher[F[_]](
    val logger: SelfAwareStructuredLogger[F],
    taskConfig: TaskConfig[OneFrameFetcherConfig],
    oneFrameService: OneFrameService[F],
    cacheService: CacheService[F],
    keyBuilder: KeyBuilder
)(
    implicit val F: Sync[F]
) extends BaseJob[F] {

  def handleOneFrameServiceError(error: OneFrameServiceError): F[List[RateResponse]] =
    logger.error(error.msg) *> List.empty[RateResponse].pure[F]

  def createKeyWithPair(pair: Rate.Pair, now: Instant): (Pair, String) =
    (pair, keyBuilder.buildCacheKey(pair, now))

  def createResponseWithPair(rate: RateResponse): (Pair, RateResponse) =
    (Pair(rate.from, rate.to), rate)

  def cachePair(pair: Rate.Pair, responseDict: Map[Pair, RateResponse], cacheKeyDict: Map[Pair, String]): F[Unit] =
    (cacheKeyDict.get(pair), responseDict.get(pair)) match {
      case (Some(key), Some(response)) => cacheService.putRate(key, response) *> F.unit
      case _                           => F.unit
    }

  override def execute: F[Unit] = {
    val pairs = Pair.getAllPair
    for {
      now <- F.delay(Instant.now())
      responses <- oneFrameService.fetchRates(pairs).toEitherT.foldF(handleOneFrameServiceError, identity(_).pure[F])
      responseDict = responses.map(createResponseWithPair).toMap
      cacheKeyDict = pairs.map(createKeyWithPair(_, now)).toMap
      _ <- pairs.traverse(cachePair(_, responseDict, cacheKeyDict))
    } yield ()
  }

  override def cronExpr: CronExpr = Cron.unsafeParse(taskConfig.expr)
}

object OneFrameFetcher {
  def apply[F[_]](logger: SelfAwareStructuredLogger[F],
                  taskConfig: TaskConfig[OneFrameFetcherConfig],
                  oneFrameService: OneFrameService[F],
                  cacheService: CacheService[F],
                  keyBuilder: KeyBuilder)(implicit F: Sync[F]) =
    new OneFrameFetcher(
      logger,
      taskConfig,
      oneFrameService,
      cacheService,
      keyBuilder
    )
}
