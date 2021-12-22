package forex.services.rates

import scala.util.control.NoStackTrace

object errors {

  sealed trait Error extends Exception with NoStackTrace
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case object EmptyResponse extends Error
  }

}
