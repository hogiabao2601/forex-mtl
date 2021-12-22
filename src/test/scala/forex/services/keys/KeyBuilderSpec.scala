package forex.services.keys

import forex.BaseSpec
import forex.domain.{ Currency, Rate }
import org.scalatest.flatspec.AsyncFlatSpec

import java.time.Instant

class KeyBuilderSpec extends AsyncFlatSpec with BaseSpec {

  "function buildCacheKey" should "return with the correct key pattern" in {
    val pair         = Rate.Pair(Currency.CHF, Currency.CAD)
    val now: Instant = Instant.parse("2021-12-30T14:22:24.00Z")
    val resultKey    = KeyBuilderOps.buildCacheKey(pair, now)
    resultKey shouldBe "2021::DECEMBER::30::14::20::CHF::CAD"
  }
}
