class AstId(location: Location, val name:String) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "ID $name\n")
    }

    override fun codeGenExpressionOrTypeName(cb: CodeBlock, context: AstBlock): Symbol {
        val ret = context.lookup(location, name)
        if (cb.pathState.isUninitialized(ret))
            Log.error(location, "Variable '$name' is uninitialized")
        else if (cb.pathState.isMaybeUninitialized(ret))
            Log.error(location, "Variable '$name' may be uninitialized")
        return ret
    }


    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val ret = codeGenExpressionOrTypeName(cb, context)
        return when(ret) {
            is SymbolError,
            is SymbolFunction,
            is SymbolIntLit,
            is SymbolRange,
            is SymbolStringLit,
            is SymbolLocalVar -> ret

            is SymbolGlobalVar -> cb.addLoad(ret.type, cb.getReg(29), ret)

            is SymbolMember -> TODO()

            is SymbolTypeName -> makeSymbolError(location, "Cannot use type name as expression")

            is SymbolReg,
            is SymbolTemp -> error("Symbol kind should not be in AST")
        }
    }

    override fun codeGenLValue(cb: CodeBlock, context: AstBlock, value: Symbol) {
        val symbol = context.lookup(location, name)
        if (!symbol.type.isTypeCompatible(value))
            Log.error(location, "Cannot assign value of type ${value.type} to variable of type ${symbol.type}")

        when(symbol) {
            is SymbolLocalVar -> {
                if (!symbol.mutable && !cb.pathState.isUninitialized(symbol))
                    if (cb.pathState.isMaybeUninitialized(symbol))
                        Log.error(location, "Immutable variable '$name' may already be initialized")
                    else
                        Log.error(location, "Cannot assign to immutable variable")
                cb.addMov(symbol, value)
                cb.pathState = cb.pathState.removeUninitialized(symbol)
            }

            is SymbolGlobalVar -> {
                if (!symbol.mutable)
                    Log.error(location, "Cannot assign to immutable variable")
                cb.addStore(value.type, value, cb.getReg(29), symbol)
            }

            is SymbolMember -> TODO()

            is SymbolError -> {}

            is SymbolRange,
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