package forex.services.rates.interpreters

import cats.effect.IO
import forex.BaseSpec
import forex.config.OneFrameConfig
import forex.domain.{ Ask, Bid, Currency, Price, Timestamp }
import forex.services.caches.CacheService
import forex.services.keys.KeyBuilderOps
import forex.services.rates.RateResponse
import forex.services.rates.errors.Error.EmptyResponse
import org.http4s.Uri
import org.http4s.client.{ Client, JavaNetClientBuilder }
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec

class OneFrameLiveSpec extends AsyncFlatSpec with BaseSpec with EitherValues {
  val oneFrameConfig             = OneFrameConfig("http://localhost", "/rates", "token")
  val httpClientStub: Client[IO] = JavaNetClientBuilder[IO](blocker).create

  val cacheServiceStub = stub[CacheService[IO]]

  val service = new OneFrameLive[IO](oneFrameConfig, httpClientStub, cacheServiceStub, KeyBuilderOps)

  "Function createRateUri" should "create a one-frame api request with input currencies" in {
    service.createRateUri("http://localhost", "/rates", Currency.AUD.toString, Currency.SGD.toString).value shouldBe Uri
      .unsafeFromString("http://localhost/rates?pair=AUDSGD")
  }

  "Function extractFirstRate" should "return the first rate when rate is not an empty list" in {

    val expected = RateResponse(
      Currency.SGD,
      Currency.AUD,
      Bid(BigDecimal(1000)),
      Ask(BigDecimal(1000)),
      Price(BigDecimal(1000)),
      Timestamp.now
    )

    val secondValue = RateResponse(
      Currency.CAD,
      Currency.GBP,
      Bid(BigDecimal(1000)),
      Ask(BigDecimal(1000)),
      Price(BigDecimal(1000)),
      Timestamp.now
    )
    val input = List(expected, secondValue)
    service.extractFirstRate(input).value shouldBe expected
  }

  it should "return an Error if the list is empty" in {
    val input = List.empty[RateResponse]
    service.extractFirstRate(input).left.value shouldBe EmptyResponse
  }
}
