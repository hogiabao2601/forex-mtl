package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect._
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain._
import forex.libs.EitherTUtils._
import forex.services.caches.CacheService
import forex.services.keys.KeyBuilder
import forex.services.oneframe.OneFrameService
import forex.services.rates.errors.Error.{ EmptyResponse, OneFrameLookupFailed }
import forex.services.rates.errors._
import forex.services.rates.{ Algebra, RateResponse }
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

import java.time.Instant

class OneFrameLive[F[_]: Sync](logger: SelfAwareStructuredLogger[F],
                               oneFrameService: OneFrameService[F],
                               cacheService: CacheService[F],
                               keyBuilder: KeyBuilder)
    extends Algebra[F] {

  /**
    * Because client only select only one pair so we need to fetch only the first element from the response
    * @param responses
    * @return
    */
  private[interpreters] def extractFirstRate(responseOpt: Option[RateResponse]): Error Either RateResponse =
    responseOpt
      .fold(
        (EmptyResponse: Error).asLeft[RateResponse]
      )(_.asRight[Error])

  /**
    * Converted the response from one-frame to our API response
    * @param rateResponse
    * @return
    */
  private[interpreters] def transformRateRateResponseToRate(rateResponse: RateResponse): Rate = {
    import io.scalaland.chimney.dsl._
    rateResponse
      .into[Rate]
      .withFieldComputed(_.pair, r => Rate.Pair(r.from, r.to))
      .withFieldRenamed(_.timeStamp, _.timestamp)
      .transform
  }

  /**
    * Get the rate from HTTP call
    * @param pair
    * @return
    */
  private[interpreters] def getRateFromApi(pair: Rate.Pair): F[Either[Error, RateResponse]] =
    oneFrameService
      .fetchRate(pair)
      .toEitherT
      .leftMap(error => OneFrameLookupFailed(error.msg))
      .flatMap(responseOpt => EitherT.fromEither(extractFirstRate(responseOpt)))
      .value

  /**
    * Handle the response from API call
    * @param key
    * @param pair
    * @return
    */
  private[interpreters] def handleApiRate(key: String, pair: Rate.Pair): F[Error Either RateResponse] = {
    val result = for {
      rate <- getRateFromApi(pair).toEitherT
      _ <- logger.info(s"handleApiRate $rate").toEitherT[Error]
      _ <- cacheService.putRate(key, rate).toEitherT[Error]
    } yield rate
    result.value
  }

  /**
    * Handle the response from cache
    * @param rateOpt
    * @param callback
    * @return
    */
  private[interpreters] def handleCacheRate(rateOpt: Option[RateResponse],
                                            callback: => F[Error Either RateResponse]): F[Error Either RateResponse] =
    rateOpt.fold(callback)(_.asRight[Error].pure[F])

  /**
    * Entry function for the response
    * @param pair
    * @return
    */
  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val result = for {
      now <- EitherT.rightT(Instant.now())
      cacheKey = keyBuilder.buildCacheKey(pair, now)
      rateOpt <- cacheService.getRate(cacheKey).toEitherT[Error]
      _ <- logger.info(s"rateOpt from cache :: $rateOpt").toEitherT[Error]
      rate <- handleCacheRate(rateOpt, handleApiRate(cacheKey, pair)).toEitherT
      result = transformRateRateResponseToRate(rate)
    } yield result
    result.value
  }
}

object OneFrameLive {

  def apply[F[_]: Sync](logger: SelfAwareStructuredLogger[F],
                        oneFrameService: OneFrameService[F],
                        cacheService: CacheService[F],
                        keyBuilder: KeyBuilder): Algebra[F] =
    new OneFrameLive[F](logger, oneFrameService, cacheService, keyBuilder)
}
