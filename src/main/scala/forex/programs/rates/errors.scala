package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

import scala.util.control.NoStackTrace

object errors {

  sealed trait Error extends Exception with NoStackTrace
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(msg)
    case RatesServiceError.WrongUri(msg)             => Error.RateLookupFailed(msg)
    case RatesServiceError.ParseError(msg)           => Error.RateLookupFailed(msg)
    case RatesServiceError.EmptyResponse             => Error.RateLookupFailed("The one frame service response is empty")
  }
}
