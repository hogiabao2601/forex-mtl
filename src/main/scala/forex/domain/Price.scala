package forex.domain

import io.circe.Decoder
import cats.syntax.either._

case class Price(value: BigDecimal) extends AnyVal

object Price {
  def apply(value: Integer): Price =
    Price(BigDecimal(value))

  implicit val priceDecoder: Decoder[Price] =
    Decoder.decodeBigDecimal.emap(Price(_).asRight[String])
}
