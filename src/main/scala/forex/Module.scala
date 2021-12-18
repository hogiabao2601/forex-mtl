package forex

import cats.effect.{ Blocker, Concurrent, ContextShift, Timer }
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.client.{ Client, _ }
import org.http4s.server.middleware.{ AutoSlash, Timeout }
import org.http4s.syntax.all._

import scala.concurrent.ExecutionContext

class Module[F[_]: Concurrent: Timer: ContextShift](config: ApplicationConfig, ec: ExecutionContext) {

  import java.util.concurrent._

  val blockingPool          = Executors.newFixedThreadPool(5)
  val blocker               = Blocker.liftExecutionContext(ec)
  val httpClient: Client[F] = JavaNetClientBuilder[F](blocker).create

  private val ratesService: RatesService[F] = RatesServices.live[F](config.oneFrame, httpClient)

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

}
