package forex

import cats.effect.{ Blocker, ContextShift, IO }
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

trait BaseSpec extends Matchers with AsyncMockFactory {
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  val blocker: Blocker              = Blocker.liftExecutionContext(ec)

}
