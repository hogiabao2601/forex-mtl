package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect._
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.show._
import forex.config.OneFrameConfig
import forex.domain._
import forex.services.caches.CacheService
import forex.services.rates.errors.Error.{ EmptyResponse, ParseError, WrongUri }
import forex.services.rates.errors._
import forex.services.rates.{ Algebra, RateResponse }
import io.circe.Json
import org.http4s.Header.Raw
import org.http4s.client.Client
import org.http4s.util.CaseInsensitiveString
import org.http4s.{ Headers, Method, Request }

import java.time.{ Instant, ZoneId }

class OneFrameLive[F[_]: Sync](oneFrameConfig: OneFrameConfig, httpClient: Client[F], cacheService: CacheService[F])
    extends Algebra[F] {
  import org.http4s.Uri

  private[interpreters] def buildCacheKey(pair: Rate.Pair): F[String] = {
    val fiveMinutes: Long = 5
    Sync[F]
      .delay(Instant.now)
      .map(
        now => {
          val zoneId = ZoneId.of("UTC")
          val zdt    = now.atZone(zoneId)
          val year   = zdt.getYear
          val month  = zdt.getMonth
          val day    = zdt.getDayOfMonth
          val hour   = zdt.getHour
          val minute = fiveMinutes * (zdt.getMinute / fiveMinutes)
          s"$year-$month-$day-$hour-$minute"
        }
      )
      .map(time => List(time, pair.from.show, pair.to.show).mkString("::"))
  }

  private[interpreters] def createRateUri(baseUri: String,
                                          ratePath: String,
                                          from: String,
                                          to: String): Error Either Uri = {
    val pairKey: String   = "pair"
    val pairValue: String = s"$from$to"
    Uri
      .fromString(baseUri)
      .map(_.withPath(ratePath))
      .map(_.withQueryParam(pairKey, pairValue))
      .leftMap(_ => WrongUri(baseUri))
  }

  private[interpreters] def callApi(request: Request[F]): F[Error Either List[RateResponse]] = {
    import org.http4s.circe._
    httpClient
      .expect[Json](request)
      .map(_.toString())
      .map { json =>
        io.circe.parser
          .decode[List[RateResponse]](json)
          .leftMap(error => {
            ParseError(error.toString)
          })
      }
  }

  private[interpreters] def extractFirstRate(responses: List[RateResponse]): Error Either RateResponse =
    responses.headOption
      .fold(
        (EmptyResponse: Error).asLeft[RateResponse]
      )(_.asRight[Error])

  private[interpreters] def transformRateRateResponseToRate(rateResponse: RateResponse): Rate = {
    import io.scalaland.chimney.dsl._
    rateResponse
      .into[Rate]
      .withFieldComputed(_.pair, r => Rate.Pair(r.from, r.to))
      .withFieldRenamed(_.timeStamp, _.timestamp)
      .transform
  }

  private[interpreters] def createRateRequest(uri: Uri, authToken: String): Request[F] =
    new Request[F](
      method = Method.GET,
      uri = uri,
      headers = Headers.of(Raw(CaseInsensitiveString("token"), authToken))
    )

  private[interpreters] def getRateFromApi(pair: Rate.Pair): F[Either[Error, RateResponse]] = {
    val rateUriE = createRateUri(oneFrameConfig.baseUri, oneFrameConfig.ratePath, pair.from.show, pair.to.show)
    val result: EitherT[F, Error, RateResponse] = for {
      rateUri <- EitherT.fromEither(rateUriE)
      request = createRateRequest(rateUri, oneFrameConfig.authToken)
      callResponse <- EitherT(callApi(request))
      rate <- EitherT.fromEither(extractFirstRate(callResponse))
    } yield rate
    result.value
  }

  private[interpreters] def handleApiRate(key: String, pair: Rate.Pair): F[Error Either RateResponse] = {
    val result = for {
      rate <- EitherT(getRateFromApi(pair))
      _ <- EitherT.right[Error](cacheService.putRate(key, rate))
    } yield rate
    result.value
  }

  private[interpreters] def handleCacheRate(rateOpt: Option[RateResponse],
                                            callback: => F[Error Either RateResponse]): F[Error Either RateResponse] =
    rateOpt.fold(callback)(_.asRight[Error].pure[F])

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val result = for {
      cacheKey <- EitherT.right[Error](buildCacheKey(pair))
      rateOpt <- EitherT.right[Error](cacheService.getRate(cacheKey))
      rate <- EitherT(handleCacheRate(rateOpt, handleApiRate(cacheKey, pair)))
      result = transformRateRateResponseToRate(rate)
    } yield result
    result.value
  }
}

object OneFrameLive {

  def apply[F[_]: Sync](oneFrameConfig: OneFrameConfig,
                        httpClient: Client[F],
                        cacheService: CacheService[F]): Algebra[F] =
    new OneFrameLive[F](oneFrameConfig, httpClient, cacheService)
}
