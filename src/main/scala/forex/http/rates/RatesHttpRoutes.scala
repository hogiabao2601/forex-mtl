package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      for {
        params <- rates.get(RatesProgramProtocol.GetRatesRequest(from, to))
        rate <- Sync[F].fromEither(params)
        response <- Ok(rate.asGetApiResponse)
      } yield response
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
