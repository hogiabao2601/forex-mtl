package forex.services.rates.interpreters

import cats.effect.IO
import forex.BaseSpec
import forex.config.OneFrameConfig
import forex.domain.{ Ask, Bid, Currency, Price, Timestamp }
import forex.services.caches.CacheService
import forex.services.keys.KeyBuilderOps
import forex.services.oneframe.OneFrameService
import forex.services.rates.RateResponse
import forex.services.rates.errors.Error.EmptyResponse
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger.getLoggerFromClass
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec

class OneFrameLiveSpec extends AsyncFlatSpec with BaseSpec with EitherValues {
  val oneFrameConfig = OneFrameConfig("http://localhost", "/rates", "token")

  val cacheServiceStub: CacheService[IO]       = stub[CacheService[IO]]
  val oneFrameServiceStub: OneFrameService[IO] = stub[OneFrameService[IO]]
  val logger: SelfAwareStructuredLogger[IO]    = getLoggerFromClass(classOf[OneFrameLiveSpec])

  val service = new OneFrameLive[IO](logger, oneFrameServiceStub, cacheServiceStub, KeyBuilderOps)
  "Function extractFirstRate" should "return the first rate when rate is not an empty list" in {

    val expected = RateResponse(
      Currency.SGD,
      Currency.AUD,
      Bid(BigDecimal(1000)),
      Ask(BigDecimal(1000)),
      Price(BigDecimal(1000)),
      Timestamp.now
    )

    val input = Some(expected)
    service.extractFirstRate(input).value shouldBe expected
  }

  it should "return an Error if the list is empty" in {
    val input: Option[RateResponse] = None
    service.extractFirstRate(input).left.value shouldBe EmptyResponse
  }
}
