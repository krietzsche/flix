restrictable enum Expr[s] {
    case Cst(Char)
    case Var(String)
    case Not((Int32, BigInt, BigDecimal))
    case And(Float64, Float32)
    case Or(Option[Bool], (Int8, Int16))
    case Xor(Array[Array[Int64, Impure], Impure], Unit)
}

mod Test.Exp.ChooseStar.SimpleTerms {



}
