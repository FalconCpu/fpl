class AstMember (
    location: Location,
    val lhs : Ast,
    val name: String,
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "MEMBER $name\n")
        lhs.dump(sb, indent + 1)
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val l = lhs.codeGenExpression(cb, context)
        if (l.type is TypeError) return l

        if (l.type is TypeString && name == "length") {
            val ret = cb.newTemp(TypeInt)
            cb.add(InstrLoad(4, ret, l, lengthSymbol))
            return ret
        }

        if (l.type !is TypeClass)
            return makeSymbolError(lhs.location, "Got type ${l.type} when expecting a class")
        val sym = l.type.members.find { it.name == name } ?: return makeSymbolError(
            location,"Member $name not found in class ${l.type}" )
        val ret = cb.newTemp(sym.type)
        when (sym) {
            is SymbolMember -> cb.add(InstrLoad(sym.type.getSize(), ret, l, sym))
            else -> Log.error(location, "Invalid field $name for member access")
        }
        return ret
    }

    override fun codeGenLValue(cb: CodeBlock, context: AstBlock, value: Symbol) {
        val l = lhs.codeGenExpression(cb, context)
        if (l.type is TypeError) return

        if (l.type !is TypeClass) {
            Log.error(lhs.location, "Got type ${l.type} when expecting a class")
            return
        }
        val sym = l.type.members.find { it.name == name } ?: run {
            Log.error(location, "Member $name not found in class ${l.type}")
            return
        }
        if (!sym.type.isTypeCompatible(value)) {
            Log.error(location, "Cannot assign value of type ${value.type} to field of type ${sym.type}")
            return
        }

        when (sym) {
            is SymbolMember -> {
                if (!sym.mutable)
                    Log.error(location, "Cannot assign to immutable field $name")
                cb.add(InstrStore(sym.type.getSize(), value, l, sym))
            }
            else -> Log.error(location, "Invalid field $name for member access")
            }
    }
}