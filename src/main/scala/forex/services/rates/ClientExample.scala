package forex.services.rates

import cats.effect.{ Blocker, ExitCode, IO, IOApp }
import cats.syntax.either._
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.WrongUri
import io.circe.generic.extras.Configuration
import io.circe.{ parser, Json }
import org.http4s.Header.Raw
import org.http4s.client.{ Client, JavaNetClientBuilder }
import org.http4s.util.CaseInsensitiveString
//import org.http4s.circe.CirceEntityDecoder._
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

import scala.concurrent.ExecutionContext

object ClientExample extends IOApp {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val decoder: Decoder[Res] = deriveConfiguredDecoder

  case class Res(from: String, to: String, bid: Double, ask: Double, price: Double, timeStamp: String)

  import io.circe.parser.decode

  val inputString =
    """[
      |{
      |    "from" : "AUD",
      |    "to" : "CAD",
      |    "bid" : 0.8036320700153536,
      |    "ask" : 0.03609871022435107,
      |    "price" : 0.419865390119852335,
      |    "time_stamp" : "2021-12-18T01:52:47.913Z"
      |  },
      |  {
      |    "from" : "AUD",
      |    "to" : "CAD",
      |    "bid" : 0.8036320700153536,
      |    "ask" : 0.03609871022435107,
      |    "price" : 0.419865390119852335,
      |    "time_stamp" : "2021-12-18T01:52:47.913Z"
      |  }
      |  ]
      |""".stripMargin

  parser.decode[List[Res]](inputString) match {
    case Right(books) => println(s"Here are the books ${books}")
    case Left(ex)     => println(s"Ooops something error ${ex}")
  }

  decode[List[Int]]("[1, 2, 3]")
//    Ok("""{"name":"Alice"}""").flatMap(_.as[User]).unsafeRunSync()
  //    import org.http4s.circe._

  import org.http4s._

//  implicit def responseMessageEntityDecoder[F[_]: Sync]: EntityDecoder[F, Res] =
//    jsonOf[F, Res](Sync[F], ResDec)

//  implicit def responseMessageEntityDecoderList[F[_]: Sync]: EntityDecoder[F, List[Res]] =
//    jsonOf[F, List[Res]](Sync[F], petDecList)

  override def run(args: List[String]): IO[ExitCode] = {
    val ec = ExecutionContext.global
    //    val blockingPool           = Executors.newFixedThreadPool(5)
    val blocker                = Blocker.liftExecutionContext(ec)
    val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create

    val baseUri  = Uri.fromString("http://localhost:8081")
    val ratePath = baseUri.map(_.withPath("/rates"))
    val withQuery: Either[Error, Uri] = ratePath
      .map(_.withQueryParam("pair", s"AUDCAD"))
      .leftMap(_ => WrongUri("10dc303535874aeccc86a8251e6992f5"))

    val request: Either[Error, Request[IO]] = withQuery.map(uri => {
      new Request[IO](
        method = Method.GET,
        uri = uri,
        headers = Headers.of(
          Raw(CaseInsensitiveString("token"), "10dc303535874aeccc86a8251e6992f5"),
          Raw(CaseInsensitiveString("Content-Type"), "application/json;charset=utf-8")
        )
      )
    })
    //    case class User(name: String)
    //    implicit val userDecoder = jsonOf[IO, User]

    import org.http4s.circe._
    val re = request.fold(throw _, identity)
    httpClient
      .expect[Json](re)
      .map(_.toString())
      .map(io.circe.parser.decode[List[Res]])

//      .map(_.as[List[Res]])
//      .map(s => {
//        println(s"s = ${s}")
//      })
      .as(ExitCode.Success)
  }
}
