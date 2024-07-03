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
        if (l.type !is TypeArray)
            return makeSymbolError(lhs.location, "index lhs must be an array not ${l.type}")
        if (!TypeInt.isTypeCompatible(r))
            return makeSymbolError(rhs.location, "array index must be an int not ${r.type}")
        val ret = cb.newTemp(l.type.base)
        val size = ret.type.getSize()
        val i1 = cb.addAluOp(AluOp.MUL_I, r, makeSymbolIntLit(size))
        val i2 = cb.addAluOp(AluOp.ADD_I, l, i1)
        cb.add(InstrLoad(size, ret, i2, symbolZero))
        return ret
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
        val size = l.type.base.getSize()
        val i1 = cb.addAluOp(AluOp.MUL_I, r, makeSymbolIntLit(size))
        val i2 = cb.addAluOp(AluOp.ADD_I, l, i1)
        cb.add(InstrStore(size, value, i2, symbolZero))
    }
}