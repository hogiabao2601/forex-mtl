package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.show._
import forex.config.OneFrameConfig
import forex.domain._
import forex.services.rates.Algebra
import forex.services.rates.errors.Error.{ EmptyResponse, ParseError, WrongUri }
import forex.services.rates.errors._
import forex.services.rates.interpreters.OneFrameLive.RateResponse
import io.circe.{ Decoder, Json }
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import org.http4s.Header.Raw
import org.http4s.client.Client
import org.http4s.util.CaseInsensitiveString
import org.http4s.{ Headers, Method, Request }

class OneFrameLive[F[_]: Sync](oneFrameConfig: OneFrameConfig, httpClient: Client[F]) extends Algebra[F] {
  import org.http4s.Uri

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
    import OneFrameLive._
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

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val rateUriE = createRateUri(oneFrameConfig.baseUri, oneFrameConfig.ratePath, pair.from.show, pair.to.show)
    val result: EitherT[F, Error, Rate] = for {
      rateUri <- EitherT.fromEither(rateUriE)
      request = createRateRequest(rateUri, oneFrameConfig.authToken)
      callResponse <- EitherT(callApi(request))
      rate <- EitherT.fromEither(extractFirstRate(callResponse))
      result = transformRateRateResponseToRate(rate)
    } yield result
    result.value
  }
}

object OneFrameLive {
  case class RateResponse(from: Currency, to: Currency, bid: Bid, ask: Ask, price: Price, timeStamp: Timestamp)

  implicit val config: Configuration          = Configuration.default.withSnakeCaseMemberNames
  implicit val decoder: Decoder[RateResponse] = deriveConfiguredDecoder

  def apply[F[_]: Sync](oneFrameConfig: OneFrameConfig, httpClient: Client[F]): Algebra[F] =
    new OneFrameLive[F](oneFrameConfig, httpClient)
}
