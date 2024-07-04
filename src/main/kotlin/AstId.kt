class AstId(location: Location, private val name:String) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "ID $name\n")
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val ret = context.lookup(location, name)
        if (ret is SymbolTypeName)
            return makeSymbolError(location, "Cannot use type name as expression")
        return ret
    }

    override fun codeGenLValue(cb: CodeBlock, context: AstBlock, value: Symbol) {
        val symbol = context.lookup(location, name)
        if (!symbol.type.isTypeCompatible(value))
            Log.error(location, "Cannot assign value of type ${value.type} to variable of type ${symbol.type}")

        when(symbol) {
            is SymbolLocalVar -> {
                if (!symbol.mutable)
                    Log.error(location, "Cannot assign to immutable variable")
                cb.add(InstrMov(symbol, value))
            }

            is SymbolGlobalVar -> {
                if (!symbol.mutable)
                    Log.error(location, "Cannot assign to immutable variable")
                TODO("Write to global variable")
            }

            is SymbolMember -> TODO()

            is SymbolError -> {}

            is SymbolReg,
            is SymbolFunction,
            is SymbolIntLit,
            is SymbolStringLit,
            is SymbolTemp,
            is SymbolTypeName -> makeSymbolError(location, "Not an lvalue")
        }
    }

    override fun resolveType(context: AstBlock): Type {
        val sym = context.lookup(location, name)
        if (sym is SymbolError || sym is SymbolTypeName)
            return sym.type
        return makeTypeError(location,"Not a type")
    }
}