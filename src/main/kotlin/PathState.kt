data class PathState (
    val uninitialized : Set<Symbol>,
    val maybeUninitialized : Set<Symbol>,
    val smartCast : Map<Symbol, Type>,
    val unreachable : Boolean
) {
    fun addUninitialized(s: Symbol) =
        copy(uninitialized = uninitialized + s, maybeUninitialized = maybeUninitialized + s)

    fun removeUninitialized(s: Symbol) =
        if (maybeUninitialized.contains(s))
            copy(uninitialized = uninitialized - s, maybeUninitialized = maybeUninitialized - s)
        else
            this

    fun addSmartCast(s: Symbol, t: Type) = copy(smartCast = smartCast + (s to t))

    fun removeSmartCast(s: Symbol) = copy(smartCast = smartCast.filterNot { it.key.dependsOn(s) } - s)

    fun addUnreachable() = copy(unreachable = true)

    fun getType(s: Symbol) : Type = smartCast[s] ?: s.type

    fun isUninitialized(s: Symbol) = uninitialized.contains(s)

    fun isMaybeUninitialized(s: Symbol) = maybeUninitialized.contains(s)
}

fun joinState(paths:List<PathState>) : PathState {
    // println("Joining $paths")
    val reachable = paths.filter { !it.unreachable }
    if (reachable.isEmpty())
        return PathState(setOf(), setOf(),  emptyMap(), true)

    val uninitialized = reachable.fold(setOf<Symbol>()) { acc, path -> acc.intersect(path.uninitialized) }

    val maybeUninitialized = reachable.fold(setOf<Symbol>()) { acc, path -> acc + path.maybeUninitialized }

    val smartCast = reachable.map { it.smartCast }
        .reduce { acc, map ->
            map.filter { (symbol, type) -> acc.containsKey(symbol) && acc[symbol]==type }
        }
    val unreachable = false
    return PathState(uninitialized, maybeUninitialized, smartCast, unreachable)
}