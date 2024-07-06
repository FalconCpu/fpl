class AstMember (
    location: Location,
    val lhs : Ast,
    val name: String,
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "MEMBER $name\n")
        lhs.dump(sb, indent + 1)
    }

    private fun codeGenTypeAccess(lhs:SymbolTypeName): Symbol {
        return when(lhs.type) {
            is TypeEnum ->
                lhs.type.members.find { it.name == name } ?:
                    makeSymbolError(location, "Enum $lhs does not have member $name")
            else -> return makeSymbolError(location, "Cannot access member $name of type ${lhs.type}")
        }
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val l = lhs.codeGenExpressionOrTypeName(cb, context)

        if (l is SymbolTypeName)
            return codeGenTypeAccess(l)     // LHS is a type name

        return when (val lt = cb.pathState.getType(l)) {
            is TypeError -> l

            is TypeString ->
                if (name == "length")
                    cb.addLoad(TypeInt, l, lengthSymbol)
                else
                    makeSymbolError(location, "Cannot access member $name of string")

            is TypeNullable ->
                if (lt.base is TypeClass)
                    makeSymbolError(location, "Cannot access member as reference could be null")
                else
                    makeSymbolError(lhs.location, "Got type ${l.type} when expecting a class")

            is TypeClass -> {
                val sym = lt.members.find { it.name == name } ?: return makeSymbolError(
                    location,"Member $name not found in class $lt" )
                return when (sym) {
                    is SymbolMember -> cb.addLoad(sym.type, l, sym)
                    else -> makeSymbolError(location, "Invalid field $name for member access")
                }
            }

            else ->
                makeSymbolError(lhs.location, "Got type $lt when expecting a class")
        }
    }

    override fun codeGenLValue(cb: CodeBlock, context: AstBlock, value: Symbol) {
        val l = lhs.codeGenExpression(cb, context)
        when (val lt = cb.pathState.getType(l)) {
            is TypeError -> {}
            is TypeNullable ->
                if (lt.base is TypeClass)
                    Log.error(location, "Cannot access member as reference could be null")
                else
                    Log.error(lhs.location, "Got type ${l.type} when expecting a class")

            is TypeClass -> {
                val sym = lt.members.find { it.name == name }
                if (sym==null)
                    return Log.error(location,"Member $name not found in class $lt" )
                when (sym) {
                    is SymbolMember -> {
                        if (!sym.mutable)
                            Log.error(location, "Cannot assign to immutable field $name")
                        cb.addStore(sym.type, value, l, sym)
                    }
                    else -> makeSymbolError(location, "Invalid field $name for member access")
                }
            }

            else ->
                makeSymbolError(lhs.location, "Got type $lt when expecting a class")
        }
    }
}