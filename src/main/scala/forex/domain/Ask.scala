package forex.domain

import io.circe.{ Decoder, Encoder, Json }
import cats.syntax.either._
case class Ask(value: BigDecimal) extends AnyVal

object Ask {
  def apply(value: Integer): Ask =
    Ask(BigDecimal(value))

  implicit val askDecoder: Decoder[Ask] =
    Decoder.decodeBigDecimal.emap(Ask(_).asRight[String])

  implicit val askEncoder: Encoder[Ask] =
    Encoder.instance[Ask](p => Json.fromBigDecimal(p.value))
}
