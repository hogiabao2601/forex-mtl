package forex.config

import forex.config.SchedulerConfig.TaskConfig

import scala.concurrent.duration.FiniteDuration
case class ApplicationConfig(
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    cache: CacheConfig,
    scheduler: SchedulerConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameConfig(
    baseUri: String,
    ratePath: String,
    authToken: String
)

case class CacheConfig(
    url: String,
    ttl: Int,
    host: String,
    port: Int,
    enable: Boolean
)

case class SchedulerConfig(
    oneFrameFetcher: TaskConfig[OneFrameFetcherConfig]
)

object SchedulerConfig {
  case class TaskConfig[T](expr: String, config: T)
  val namespace: String = "scheduler"
}

case class OneFrameFetcherConfig()
