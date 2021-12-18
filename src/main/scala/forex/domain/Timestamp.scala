package forex.domain

import io.circe.{ Decoder, Encoder, Json }
import cats.syntax.either._

import java.time.OffsetDateTime

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val timestampDecoder: Decoder[Timestamp] =
    Decoder.decodeOffsetDateTime.emap(Timestamp(_).asRight[String])

  implicit val timestampEncoder: Encoder[Timestamp] =
    Encoder.instance[Timestamp](p => Json.fromString(p.value.toString))
}
