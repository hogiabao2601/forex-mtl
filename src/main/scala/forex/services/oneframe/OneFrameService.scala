package forex.services.oneframe

import forex.domain.Rate
import forex.services.oneframe.OneFrameService.OneFrameServiceError
import forex.services.rates.RateResponse

import scala.util.control.NoStackTrace

trait OneFrameService[F[_]] {
  def fetchRate(pair: Rate.Pair): F[OneFrameServiceError Either Option[RateResponse]]
  def fetchRates(pairs: List[Rate.Pair]): F[OneFrameServiceError Either List[RateResponse]]
}

object OneFrameService {
  sealed trait OneFrameServiceError extends Exception with NoStackTrace {
    def msg: String
  }
  object OneFrameServiceError {
    final case class WrongUri(msg: String) extends OneFrameServiceError
    final case class ParseError(msg: String) extends OneFrameServiceError

  }

}
