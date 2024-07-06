sealed class AstBlock (
    location: Location,
    val parent: AstBlock?
) : Ast(location) {
    private val symbolTable = mutableMapOf<String, Symbol>()
    protected val statements = mutableListOf<Ast>()

    fun add(location: Location, symbol: Symbol) {
        val duplicate = symbolTable[symbol.name]
        if (duplicate != null)
            Log.error(location, "Duplicate symbol '$symbol'")
        symbolTable[symbol.name] = symbol
    }

    fun lookup(location:Location, name:String) : Symbol {
        return predefinedSymbols[name]
            ?: symbolTable[name]
            ?: parent?.lookup(location, name)
            ?: run {
                val ret = makeSymbolError(location, "Symbol '$name' not found")
                symbolTable[name] = ret
                ret
            }
    }

    open fun add(statement: Ast) {
        if (statement is AstFunction)
            Log.error(statement.location, "Functions are not allowed to nest")
        statements += statement
    }


    fun findEnclosingFunction(): AstFunction? {
        var p : AstBlock? = this
        while (p != null) {
            if (p is AstFunction)
                return p
            p = p.parent
        }
        return null
    }

    fun calculateGlobalOffsets() {
        var offset = 0
        for(sym in symbolTable.values) {
            if (sym is SymbolGlobalVar) {
                sym.offset = offset
                offset += sym.type.getSize()
                offset = ((offset-1) or 3) +1 // Align to 4 bytes
            }
        }
    }

}