package forex.services.oneframe.interpreters

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits.toShow
import cats.syntax.either._
import cats.syntax.functor._
import forex.config.OneFrameConfig
import forex.libs.EitherTUtils._
import forex.domain.Rate
import forex.services.oneframe.OneFrameService
import forex.services.oneframe.OneFrameService.OneFrameServiceError
import forex.services.oneframe.OneFrameService.OneFrameServiceError.{ ParseError, WrongUri }
import forex.services.oneframe.interpreters.OneFrameServiceImpl.createRateUri
import forex.services.rates.RateResponse
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.circe.Json
import org.http4s.Header.Raw
import org.http4s.client.Client
import org.http4s.util.CaseInsensitiveString
import org.http4s.{ Headers, Method, Request, Uri }

class OneFrameServiceImpl[F[_]: Sync](logger: SelfAwareStructuredLogger[F],
                                      oneFrameConfig: OneFrameConfig,
                                      httpClient: Client[F])
    extends OneFrameService[F] {

  /**
    * Use the http4s client to call the request
    * @param request
    * @return
    */
  private[interpreters] def callApi(request: Request[F]): F[OneFrameServiceError Either List[RateResponse]] = {
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

  override def fetchRate(pair: Rate.Pair): F[Either[OneFrameServiceError, Option[RateResponse]]] = {
    val result = for {
      uri <- EitherT.fromEither(createRateUri(oneFrameConfig.baseUri, oneFrameConfig.ratePath, List(pair)))
      _ <- logger.info(s"fetchRate uri :: $uri").toEitherT
      request = createRateRequest(uri, oneFrameConfig.authToken)
      response <- callApi(request).toEitherT
    } yield response.headOption
    result.value
  }

  override def fetchRates(pairs: List[Rate.Pair]): F[Either[OneFrameServiceError, List[RateResponse]]] = {
    val result = for {
      uri <- EitherT.fromEither(createRateUri(oneFrameConfig.baseUri, oneFrameConfig.ratePath, pairs))
      _ <- logger.info(s"fetchRates uri :: $uri").toEitherT
      request = createRateRequest(uri, oneFrameConfig.authToken)
      response <- EitherT(callApi(request))
    } yield response
    result.value
  }

}

object OneFrameServiceImpl {
  def addCurrencyPair(baseUri: Uri, pairs: List[Rate.Pair]): Uri = {
    val pairKey: String = "pair"
    val values          = pairs.map(pair => s"${pair.from.show}${pair.to.show}")
    val params          = Map(pairKey -> values)
    baseUri.withMultiValueQueryParams(params)
  }

  /**
    * Build the request URI from app config and user request
    * @param baseUri
    * @param ratePath
    * @return
    */
  def createRateUri(baseUri: String, ratePath: String, pairs: List[Rate.Pair]): OneFrameServiceError Either Uri =
    Uri
      .fromString(baseUri)
      .map(_.withPath(ratePath))
      .map(addCurrencyPair(_, pairs))
      .leftMap(_ => WrongUri(baseUri))
}
