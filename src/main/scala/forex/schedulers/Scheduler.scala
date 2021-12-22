package forex.schedulers

import cats.effect.{ ConcurrentEffect, Timer }
import cron4s.expr.CronExpr
import eu.timepit.fs2cron.ScheduledStreams
import eu.timepit.fs2cron.cron4s.Cron4sScheduler

class Scheduler[F[_]](jobs: List[BaseJob[F]])(implicit F: ConcurrentEffect[F], timer: Timer[F]) {

  val streams: ScheduledStreams[F, CronExpr] = new ScheduledStreams(Cron4sScheduler.systemDefault[F])

  def run(): fs2.Stream[F, Unit] =
    streams.schedule(jobs.map(_.toTask))
}
