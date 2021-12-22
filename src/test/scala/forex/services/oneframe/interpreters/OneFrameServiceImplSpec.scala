package forex.services.oneframe.interpreters

import forex.BaseSpec
import forex.domain.Currency
import forex.domain.Rate.Pair
import org.http4s.Uri
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec

class OneFrameServiceImplSpec extends AsyncFlatSpec with BaseSpec with EitherValues {
  "Function createRateUri" should "create a one-frame api request with input currencies" in {
    OneFrameServiceImpl
      .createRateUri("http://localhost", "/rates", List(Pair(Currency.AUD, Currency.SGD)))
      .value shouldBe Uri
      .unsafeFromString("http://localhost/rates?pair=AUDSGD")
  }

}
