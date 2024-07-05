class AvailableMap(cb:CodeBlock) {
    // Build a map indicating which temp vars are still valid at a given point in the program

    val allTemps = cb.symbols.filterIsInstance<SymbolTemp>()


}