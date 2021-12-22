package forex.services.keys

import cats.implicits.toShow
import forex.domain.Rate

import java.time.{ Instant, ZoneId }

trait KeyBuilder {

  /**
    * Build the cache key based one the Pair and current time, we rounded the minutes so the key always round with 5 mins
    * The key pattern example: 2021-DECEMBER-20-12-5::AAA::BBB
    * @param pair
    *  @param now
    * @return
    */
  def buildCacheKey(pair: Rate.Pair, now: Instant): String = {
    val fiveMinutes: Long = 5
    val zoneId            = ZoneId.of("UTC")
    val zdt               = now.atZone(zoneId)
    val year              = zdt.getYear
    val month             = zdt.getMonth
    val day               = zdt.getDayOfMonth
    val hour              = zdt.getHour
    val minute            = fiveMinutes * (zdt.getMinute / fiveMinutes)
    List(year, month, day, hour, minute, pair.from.show, pair.to.show).mkString("::")
  }

}

object KeyBuilderOps extends KeyBuilder
