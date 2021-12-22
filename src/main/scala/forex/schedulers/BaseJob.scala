package forex.schedulers

import forex.libs
import cats.effect.Sync
import cron4s.CronExpr
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import fs2.Stream

trait BaseJob[F[_]] {
  implicit def F: Sync[F]
  import cats.syntax.applicativeError._

  def logger: SelfAwareStructuredLogger[F]

  def cronExpr: CronExpr

  def execute: F[Unit]

  final def toTask: (CronExpr, Stream[F, Unit]) =
    cronExpr -> Stream.eval(execute.handleErrorWith(handleError))

  protected def handleError(error: Throwable): F[Unit] =
    logger.error(libs.StringUtils.printThrowable(error))
}
