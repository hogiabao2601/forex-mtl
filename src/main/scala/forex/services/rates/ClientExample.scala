//package forex.services.rates
//
//import cats.effect.{IO, IOApp}
//import scalacache.memoization.memoize
//
//
//object ClientExample extends IOApp {
//  import scalacache.memcached._
//  import scalacache._
//
//  def getUser(userId: Int): Cat = {
//    val cacheResult = get("")
//    if (cacheResult.isEmpty) {
//      val fromDB = queryUserFromDB(userId)
//      put(buildUserKey(userId))(fromDB)
//      fromDB
//    } else cacheResult.get
//  }
//
//  // We'll use the binary serialization codec - more on that later
//
//  implicit val mode: Mode[IO] = scalacache.CatsEffect.modes.async
////  def getUserCatsIO(id: Int): IO[Cat] = {
////    get[IO, Cat]("dasdasd")
////  {
////    memoize[IO, Cat](None) {
////      Cat(id, "io-user")
////    }
////  }
////  }
//
//  import scalacache.serialization.circe._
//  import scalacache.serialization.circe._
//  import io.circe.generic.auto._
//
//  final case class Cat(id: Int, name: String)
//
//  implicit val catsCache: Cache[Cat] = MemcachedCache.apply[Cat]("localhost:11211")
//  case class User(id: Long, name: String)
////      .as(ExitCode.Success)
//  }
//}
