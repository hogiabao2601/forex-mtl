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
import forex.services.keys.KeyBuilder
import forex.services.rates.errors.Error.{ EmptyResponse, ParseError, WrongUri }
import forex.services.rates.errors._
import forex.services.rates.{ Algebra, RateResponse }
import io.circe.Json
import org.http4s.Header.Raw
import org.http4s.client.Client
import org.http4s.util.CaseInsensitiveString
import org.http4s.{ Headers, Method, Request }

import java.time.Instant

class OneFrameLive[F[_]: Sync](oneFrameConfig: OneFrameConfig,
                               httpClient: Client[F],
                               cacheService: CacheService[F],
                               keyBuilder: KeyBuilder)
    extends Algebra[F] {
  import org.http4s.Uri

  /**
    * Build the request URI from app config and user request
    * @param baseUri
    * @param ratePath
    * @param from
    * @param to
    * @return
    */
  protected[interpreters] def createRateUri(baseUri: String,
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

  /**
    * Use the http4s client to call the request
    * @param request
    * @return
    */
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

  /**
    * Because client only select only one pair so we need to fetch only the first element from the response
    * @param responses
    * @return
    */
  private[interpreters] def extractFirstRate(responses: List[RateResponse]): Error Either RateResponse =
    responses.headOption
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
    * Create a request from built URI and authentication token
    * @param uri
    * @param authToken
    * @return
    */
  private[interpreters] def createRateRequest(uri: Uri, authToken: String): Request[F] =
    new Request[F](
      method = Method.GET,
      uri = uri,
      headers = Headers.of(Raw(CaseInsensitiveString("token"), authToken))
    )

  /**
    * Get the rate from HTTP call
    * @param pair
    * @return
    */
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

  /**
    * Handle the response from API call
    * @param key
    * @param pair
    * @return
    */
  private[interpreters] def handleApiRate(key: String, pair: Rate.Pair): F[Error Either RateResponse] = {
    val result = for {
      rate <- EitherT(getRateFromApi(pair))
      _ <- EitherT.right[Error](cacheService.putRate(key, rate))
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
                        cacheService: CacheService[F],
                        keyBuilder: KeyBuilder): Algebra[F] =
    new OneFrameLive[F](oneFrameConfig, httpClient, cacheService, keyBuilder)
}
