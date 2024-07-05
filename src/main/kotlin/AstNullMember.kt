class AstNullMember  (
    location: Location,
    val lhs : Ast,
    val name: String,
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "NULLMEMBER $name\n")
        lhs.dump(sb, indent + 1)
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val l = lhs.codeGenExpression(cb, context)
        if (l.type is TypeError) return l
        if (l.type !is TypeNullable)
            return makeSymbolError(lhs.location,"Got type ${l.type} when expecting a nullable")
        val lt = l.type.base
        if (lt !is TypeClass)
            return makeSymbolError(lhs.location,"Got type ${l.type} when expecting a class")

        println("members ${lt.members}")
        val sym = lt.members.find { it.name == name } ?:
            return makeSymbolError(location,"Member $name not found in class $lt" )
        if (sym !is SymbolMember)
            return makeSymbolError(location, "Invalid field $name for member access")

        val labelDone = cb.newLabel()
        val ret = cb.addCopy(symbolZero, makeTypeNullable(sym.type))
        cb.addBranch(AluOp.EQ_I, labelDone, l, symbolZero)
        val ld = cb.addLoad(makeTypeNullable(sym.type), l, sym)
        cb.addMov(ret,ld)
        cb.addLabel(labelDone)
        return ret
    }

    override fun codeGenLValue(cb: CodeBlock, context: AstBlock, value: Symbol) {
        val l = lhs.codeGenExpression(cb, context)
        if (l.type is TypeError) return
        if (l.type !is TypeNullable)
            return Log.error(lhs.location, "Got type ${l.type} when expecting a nullable")
        val lt = l.type.base
        if (lt !is TypeClass)
            return Log.error(lhs.location, "Got type ${l.type} when expecting a class")

        val sym =
            lt.members.find { it.name == name } ?: return Log.error(location, "Member $name not found in class $lt")
        if (sym !is SymbolMember)
            return Log.error(location, "Invalid field $name for member access")

        val labelDone = cb.newLabel()
        cb.addBranch(AluOp.EQ_I, labelDone, l, symbolZero)
        cb.addStore(sym.type, value, l, sym)
        cb.addLabel(labelDone)
    }
}