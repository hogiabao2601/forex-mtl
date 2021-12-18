package forex.domain

import io.circe.Decoder
import cats.syntax.either._
case class Bid(value: BigDecimal) extends AnyVal

object Bid {
  def apply(value: Integer): Bid =
    Bid(BigDecimal(value))

  implicit val bidDecoder: Decoder[Bid] =
    Decoder.decodeBigDecimal.emap(Bid(_).asRight[String])

}
