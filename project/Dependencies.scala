import sbt._

object Dependencies {

  object Versions {
    val cats       = "2.5.0"
    val catsEffect = "2.4.1"
    val fs2        = "2.5.4"
    val http4s     = "0.21.22"
    val circe      = "0.13.0"
    val pureConfig = "0.14.1"

    val kindProjector  = "0.10.3"
    val logback        = "1.2.3"
    val scalaCheck     = "1.15.3"
    val scalaTest      = "3.2.7"
    val catsScalaCheck = "0.3.0"
    val scalaCache     = "0.28.0"
    val chimney        = "0.6.1"
    val scalamock      = "5.1.0"
    val log4Cats       = "1.1.1"
    val enumeratum     = "1.7.0"
    val fs2Cron        = "0.5.0"
  }

  object Libraries {
    def circe(artifact: String): ModuleID      = "io.circe"          %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID     = "org.http4s"        %% artifact % Versions.http4s
    def log4Cats(artifact: String): ModuleID   = "io.chrisdavenport" %% artifact % Versions.log4Cats
    def enumeratum(artifact: String): ModuleID = "com.beachape"      %% artifact % Versions.enumeratum
    def fs2Cron(artifact: String): ModuleID    = "eu.timepit"        %% artifact % Versions.fs2Cron

    lazy val cats       = "org.typelevel" %% "cats-core"   % Versions.cats
    lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
    lazy val fs2        = "co.fs2"        %% "fs2-core"    % Versions.fs2

    lazy val http4sDsl       = http4s("http4s-dsl")
    lazy val http4sServer    = http4s("http4s-blaze-server")
    lazy val http4sClient    = http4s("http4s-blaze-client")
    lazy val http4sCirce     = http4s("http4s-circe")
    lazy val circeCore       = circe("circe-core")
    lazy val circeGeneric    = circe("circe-generic")
    lazy val circeGenericExt = circe("circe-generic-extras")
    lazy val circeParser     = circe("circe-parser")
    lazy val pureConfig      = "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig
    // Compiler plugins
    lazy val kindProjector = "org.typelevel" %% "kind-projector" % Versions.kindProjector

    // Runtime
    lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

    // Test
    lazy val scalaTest      = "org.scalatest"     %% "scalatest"       % Versions.scalaTest
    lazy val scalaCheck     = "org.scalacheck"    %% "scalacheck"      % Versions.scalaCheck
    lazy val catsScalaCheck = "io.chrisdavenport" %% "cats-scalacheck" % Versions.catsScalaCheck
    lazy val scalamock      = "org.scalamock"     %% "scalamock"       % Versions.scalamock //Apache License 2.0

    lazy val scalaCacheCatsEffect = "com.github.cb372" %% "scalacache-cats-effect" % Versions.scalaCache
    lazy val scalaCacheMemcached  = "com.github.cb372" %% "scalacache-memcached" % Versions.scalaCache
    lazy val scalaCacheRedis      = "com.github.cb372" %% "scalacache-redis" % Versions.scalaCache
    lazy val scalaCacheCirce      = "com.github.cb372" %% "scalacache-circe" % Versions.scalaCache
    lazy val chimney              = "io.scalaland" %% "chimney" % Versions.chimney
    lazy val log4CatsCore         = log4Cats("log4cats-core")
    lazy val log4CatsSlf4j        = log4Cats("log4cats-slf4j")
    lazy val enumeratumCirce      = enumeratum("enumeratum-circe")
    lazy val fs2CronCore          = fs2Cron("fs2-cron-core")
    lazy val fs2CronCron4s        = fs2Cron("fs2-cron-cron4s")
  }

}
