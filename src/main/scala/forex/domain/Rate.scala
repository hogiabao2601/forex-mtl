package forex.domain

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    def getAllPair: List[Pair] =
      (for {
        v1 <- Currency.values
        v2 <- Currency.values
        if v1 != v2
      } yield Pair(v1, v2)).toList
  }
}
