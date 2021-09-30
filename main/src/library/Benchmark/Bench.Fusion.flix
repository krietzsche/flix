use Benchmark.Benchmark;
use Benchmark.defBenchmark;

///
/// References:
///
/// https://github.com/cs-au-dk/streamliner/blob/master/src/main/java/dk/casa/streamliner/jmh/
/// https://github.com/biboudis/streamalg/blob/master/src/main/java/benchmarks/
///

///////////////////////////////////////////////////////////////////////////////
/// length                                                                  ///
///////////////////////////////////////////////////////////////////////////////
pub def listLength(l: Array[Int32]): Benchmark =
    defBenchmark("List.length", () -> {
        l |>
        Array.toList |>
        List.length
    } as & Pure)

pub def lazyListLength(l: Array[Int32]): Benchmark =
    defBenchmark("LazyList.length", () -> {
        l |>
        Array.toLazyList |>
        LazyList.length
    } as & Pure)

pub def streamLength(l: Array[Int32]): Benchmark =
    defBenchmark("Stream.length", () -> {
        l |>
        Array.toStream |>
        Stream.length
    })

///////////////////////////////////////////////////////////////////////////////
/// filter length                                                           ///
///////////////////////////////////////////////////////////////////////////////
pub def listFilterLength(l: Array[Int32]): Benchmark =
    defBenchmark("List.filterLength", () -> {
        l |>
        Array.toList |>
        List.filter(x -> x % 2 == 0) |>
        List.length
    } as & Pure)

pub def lazyListFilterLength(l: Array[Int32]): Benchmark =
    defBenchmark("LazyList.filterLength", () -> {
        l |>
        Array.toLazyList |>
        LazyList.filter(x -> x % 2 == 0) |>
        LazyList.length
    } as & Pure)

pub def streamFilterLength(l: Array[Int32]): Benchmark =
    defBenchmark("Stream.filterLength", () -> {
        l |>
        Array.toStream |>
        Stream.filter(x -> x % 2 == 0) |>
        Stream.length
    } as & Pure)

///////////////////////////////////////////////////////////////////////////////
/// filter map length                                                       ///
///////////////////////////////////////////////////////////////////////////////
pub def listFilterMapLength(l: Array[Int32]): Benchmark =
    defBenchmark("List.filterMapLength", () -> {
        l |>
        Array.toList |>
        List.filter(x -> x % 2 == 0) |>
        List.map(x -> x * x) |>
        List.length
    } as & Pure)

pub def lazyListFilterMapLength(l: Array[Int32]): Benchmark =
    defBenchmark("LazyList.filterMapLength", () -> {
        l |>
        Array.toLazyList |>
        LazyList.filter(x -> x % 2 == 0) |>
        LazyList.map(x -> x * x) |>
        LazyList.length
    } as & Pure)

pub def streamFilterMapLength(l: Array[Int32]): Benchmark =
    defBenchmark("Stream.filterMapLength", () -> {
        l |>
        Array.toStream |>
        Stream.filter(x -> x % 2 == 0) |>
        Stream.map(x -> x * x) |>
        Stream.length
    } as & Pure)

///////////////////////////////////////////////////////////////////////////////
/// sum                                                                     ///
///////////////////////////////////////////////////////////////////////////////
pub def listSum(l: Array[Int32]): Benchmark =
    defBenchmark("List.sum", () -> {
        l |>
        Array.toList |>
        List.foldLeft((x, y) -> x + y, 0)
    } as & Pure)

pub def lazyListSum(l: Array[Int32]): Benchmark =
    defBenchmark("LazyList.sum", () -> {
        l |>
        Array.toLazyList |>
        LazyList.foldLeft((x, y) -> x + y, 0)
    } as & Pure)

///////////////////////////////////////////////////////////////////////////////
/// filter sum                                                              ///
///////////////////////////////////////////////////////////////////////////////
pub def listFilterSum(l: Array[Int32]): Benchmark =
    defBenchmark("List.filterSum", () -> {
        l |>
        Array.toList |>
        List.filter(x -> x % 2 == 0) |>
        List.foldLeft((x, y) -> x + y, 0)
    } as & Pure)

pub def lazyListFilterSum(l: Array[Int32]): Benchmark =
    defBenchmark("LazyList.filterSum", () -> {
        l |>
        Array.toLazyList |>
        LazyList.filter(x -> x % 2 == 0) |>
        LazyList.foldLeft((x, y) -> x + y, 0)
    } as & Pure)

///////////////////////////////////////////////////////////////////////////////
/// filter map sum                                                          ///
///////////////////////////////////////////////////////////////////////////////
pub def listFilterMapSum(l: Array[Int32]): Benchmark =
    defBenchmark("List.filterMapSum", () -> {
        l |>
        Array.toList |>
        List.filter(x -> x % 2 == 0) |>
        List.map(x -> x * x) |>
        List.foldLeft((x, y) -> x + y, 0)
    } as & Pure)

pub def lazyListFilterMapSum(l: Array[Int32]): Benchmark =
    defBenchmark("LazyList.filterMapSum", () -> {
        l |>
        Array.toLazyList |>
        LazyList.filter(x -> x % 2 == 0) |>
        LazyList.map(x -> x * x) |>
        LazyList.foldLeft((x, y) -> x + y, 0)
    } as & Pure)

///////////////////////////////////////////////////////////////////////////////
/// filter8                                                                 ///
///////////////////////////////////////////////////////////////////////////////
pub def listFilter8(l: Array[Int32]): Benchmark =
    defBenchmark("List.filter8", () -> {
        l |>
        Array.toList |>
        List.filter(x -> x > 1) |>
        List.filter(x -> x > 2) |>
        List.filter(x -> x > 3) |>
        List.filter(x -> x > 4) |>
        List.filter(x -> x > 5) |>
        List.filter(x -> x > 6) |>
        List.filter(x -> x > 7) |>
        List.filter(x -> x > 8) |>
        List.length
    } as & Pure)

pub def lazyListFilter8(l: Array[Int32]): Benchmark =
    defBenchmark("LazyList.filter8", () -> {
        l |>
        Array.toLazyList |>
        LazyList.filter(x -> x > 1) |>
        LazyList.filter(x -> x > 2) |>
        LazyList.filter(x -> x > 3) |>
        LazyList.filter(x -> x > 4) |>
        LazyList.filter(x -> x > 5) |>
        LazyList.filter(x -> x > 6) |>
        LazyList.filter(x -> x > 7) |>
        LazyList.filter(x -> x > 8) |>
        LazyList.length
    } as & Pure)

pub def streamFilter8(l: Array[Int32]): Benchmark =
    defBenchmark("Stream.filter8", () -> {
        l |>
        Array.toStream |>
        Stream.filter(x -> x > 1) |>
        Stream.filter(x -> x > 2) |>
        Stream.filter(x -> x > 3) |>
        Stream.filter(x -> x > 4) |>
        Stream.filter(x -> x > 5) |>
        Stream.filter(x -> x > 6) |>
        Stream.filter(x -> x > 7) |>
        Stream.filter(x -> x > 8) |>
        Stream.length
    } as & Pure)

///////////////////////////////////////////////////////////////////////////////
/// map8                                                                    ///
///////////////////////////////////////////////////////////////////////////////
pub def listMap8(l: Array[Int32]): Benchmark =
    defBenchmark("List.map8", () -> {
        l |>
        Array.toList |>
        List.map(x -> x + 1) |>
        List.map(x -> x + 2) |>
        List.map(x -> x + 3) |>
        List.map(x -> x + 4) |>
        List.map(x -> x + 5) |>
        List.map(x -> x + 6) |>
        List.map(x -> x + 7) |>
        List.map(x -> x + 8) |>
        List.length
    } as & Pure)

pub def lazyListMap8(l: Array[Int32]): Benchmark =
    defBenchmark("LazyList.map8", () -> {
        l |>
        Array.toLazyList |>
        LazyList.map(x -> x + 1) |>
        LazyList.map(x -> x + 2) |>
        LazyList.map(x -> x + 3) |>
        LazyList.map(x -> x + 4) |>
        LazyList.map(x -> x + 5) |>
        LazyList.map(x -> x + 6) |>
        LazyList.map(x -> x + 7) |>
        LazyList.map(x -> x + 8) |>
        LazyList.length
    } as & Pure)

pub def streamMap8(l: Array[Int32]): Benchmark =
    defBenchmark("Stream.map8", () -> {
        l |>
        Array.toStream |>
        Stream.map(x -> x + 1) |>
        Stream.map(x -> x + 2) |>
        Stream.map(x -> x + 3) |>
        Stream.map(x -> x + 4) |>
        Stream.map(x -> x + 5) |>
        Stream.map(x -> x + 6) |>
        Stream.map(x -> x + 7) |>
        Stream.map(x -> x + 8) |>
        Stream.length
    } as & Pure)

///////////////////////////////////////////////////////////////////////////////
/// flatMap take sum                                                        ///
///////////////////////////////////////////////////////////////////////////////
pub def listFlatMapTakeSum(l: Array[Int32]): Benchmark =
    let l2 = List.range(1, 10);
    defBenchmark("List.flatMapTakeSum", () -> {
        l |>
        Array.toList |>
        List.flatMap(x -> List.map(y -> x * y, l2)) |>
        List.take(100_000) |>  // TODO
        List.foldLeft((x, y) -> x + y, 0)
    } as & Pure)

pub def lazyListFlatMapTakeSum(l: Array[Int32]): Benchmark =
    let l2 = LazyList.range(1, 10);
    defBenchmark("LazyList.flatMapTakeSum", () -> {
        l |>
        Array.toLazyList |>
        LazyList.flatMap(x -> LazyList.map(y -> x * y, l2)) |>
        LazyList.take(100_000) |>  // TODO
        LazyList.foldLeft((x, y) -> x + y, 0)
    } as & Pure)


///////////////////////////////////////////////////////////////////////////////
/// cartesian                                                               ///
///////////////////////////////////////////////////////////////////////////////
pub def listCartesian(l: Array[Int32]): Benchmark =
    let l2 = List.range(1, 10);
    defBenchmark("List.cartesian", () -> {
        l |>
        Array.toList |>
        List.flatMap(x -> List.map(y -> x * y, l2)) |>
        List.foldLeft((x, y) -> x + y, 0)
    } as & Pure)

pub def lazyListCartesian(l: Array[Int32]): Benchmark =
    let l2 = LazyList.range(1, 10);
    defBenchmark("LazyList.cartesian", () -> {
        l |>
        Array.toLazyList |>
        LazyList.flatMap(x -> LazyList.map(y -> x * y, l2)) |>
        LazyList.foldLeft((x, y) -> x + y, 0)
    } as & Pure)

///////////////////////////////////////////////////////////////////////////////
/// Initial Input                                                           ///
///////////////////////////////////////////////////////////////////////////////

///
/// Returns a list of `n` concatenated lists with elements from `1` to `1_000`.
///
pub def input(n: Int32): Array[Int32] & Impure =
    Array.range(1, n) |>
    Array.flatMap(_ -> Array.range(1, 1_000))

///////////////////////////////////////////////////////////////////////////////
/// Biboudis Suite                                                          ///
///////////////////////////////////////////////////////////////////////////////
pub def biboudisSuite(n: Int32): Array[Benchmark] & Impure =
    let l = input(n);
    [
        // List
        listLength(l),
        listFilterLength(l),
        listFilterMapLength(l),
        listSum(l),
        listFilterSum(l),
        listFilterMapSum(l),
        listFilter8(l),
        listMap8(l),
        listFlatMapTakeSum(l),
        listCartesian(l),

        // LazyList
        lazyListLength(l),
        lazyListFilterLength(l),
        lazyListFilterMapLength(l),
        lazyListSum(l),
        lazyListFilterSum(l),
        lazyListFilterMapSum(l),
        lazyListFilter8(l),
        lazyListMap8(l),
        lazyListFlatMapTakeSum(l),
        lazyListCartesian(l),

        // Stream
        streamLength(l),
        streamFilterLength(l),
        streamFilterMapLength(l),
        streamFilter8(l),
        streamMap8(l)

    ]

def main(_args: Array[String]) : Int32 & Impure =
    let s1 = biboudisSuite(100);
    Benchmark.runWithBudget(s1, 250_000_000_000i64);
    0


// TODO: Remove
pub def fromList(l: List[a]): Stream[a, List[a]] =
    let next = s -> match s {
        case Nil     => Done
        case x :: xs => Yield(x, xs)
    };
    Stream(next, l)