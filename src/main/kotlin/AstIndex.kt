class AstIndex(
    location: Location,
    private val lhs: Ast,
    private val rhs: Ast
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "index\n")
        lhs.dump(sb,indent+1)
        rhs.dump(sb,indent+1)
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val r = rhs.codeGenExpression(cb, context)
        val l = lhs.codeGenExpression(cb, context)
        if (!TypeInt.isTypeCompatible(r))
            return makeSymbolError(rhs.location, "array index must be an int not ${r.type}")

        if (l.type is TypeString) {
            val i2 = cb.addAluOp(AluOp.ADD_I, l, r, TypeInt)
            return cb.addLoad(TypeChar, i2, symbolZero)
        }

        if (l.type !is TypeArray)
            return makeSymbolError(lhs.location, "index lhs must be an array not ${l.type}")
        val type = l.type.base
        val offset = cb.addAluOp(AluOp.MUL_I, r, makeSymbolIntLit(type.getSize()), TypeInt)
        val addr = cb.addAluOp(AluOp.ADD_I, l, offset, TypeAddress)
        return cb.addLoad(type, addr, symbolZero)
    }

    override fun codeGenLValue(cb: CodeBlock, context: AstBlock, value: Symbol) {
        val r = rhs.codeGenExpression(cb, context)
        val l = lhs.codeGenExpression(cb, context)
        if (l.type !is TypeArray) {
            Log.error(lhs.location, "index lhs must be an array not ${l.type}")
            return
        }
        if (!TypeInt.isTypeCompatible(r))
            Log.error(rhs.location, "array index must be an int not ${r.type}")
        val type = l.type.base
        val offset = cb.addAluOp(AluOp.MUL_I, r, makeSymbolIntLit(type.getSize()), TypeInt)
        val addr = cb.addAluOp(AluOp.ADD_I, l, offset, TypeAddress)
        cb.addStore(type, value, addr, symbolZero)
    }
}