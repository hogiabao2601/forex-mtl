package forex.domain

import cats.syntax.either._
import io.circe.{ Decoder, Encoder, Json }
case class Bid(value: BigDecimal) extends AnyVal

object Bid {
  def apply(value: Integer): Bid =
    Bid(BigDecimal(value))

  implicit val bidDecoder: Decoder[Bid] =
    Decoder.decodeBigDecimal.emap(Bid(_).asRight[String])

  implicit val bidEncoder: Encoder[Bid] =
    Encoder.instance[Bid](p => Json.fromBigDecimal(p.value))

}
