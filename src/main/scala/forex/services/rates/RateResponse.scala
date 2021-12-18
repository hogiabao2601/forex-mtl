package forex.services.rates

import forex.domain.{ Ask, Bid, Currency, Price, Timestamp }
import io.circe.generic.extras.Configuration
import io.circe.{ Decoder, Encoder }
import io.circe.generic.extras.semiauto.{ deriveConfiguredDecoder, deriveConfiguredEncoder }

case class RateResponse(from: Currency, to: Currency, bid: Bid, ask: Ask, price: Price, timeStamp: Timestamp)

object RateResponse {
  implicit val config: Configuration          = Configuration.default.withSnakeCaseMemberNames
  implicit val decoder: Decoder[RateResponse] = deriveConfiguredDecoder
  implicit val encoder: Encoder[RateResponse] = deriveConfiguredEncoder
}
