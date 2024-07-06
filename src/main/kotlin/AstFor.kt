class AstFor (
    location: Location,
    parent : AstBlock,
    val id: String,
    val rangeAst: Ast
) : AstBlock(location,parent) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "FOR $id\n")
        rangeAst.dump(sb, indent + 1)
        statements.forEach { it.dump(sb, indent + 1) }
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        val range = rangeAst.codeGenExpression(cb, context)
        if (range !is SymbolRange) {
            Log.error(location, "FOR loop range must be a range")
            return
        }
        check(range.type is TypeRange)

        val symbol = SymbolLocalVar(id, range.type.base, false)
        add(location, symbol)
        cb.add(symbol)

        val labelCond = cb.newLabel()
        val labelStart = cb.newLabel()
        val braOp = if (range.type.inclusive) AluOp.LTE_I else AluOp.LT_I
        cb.addMov(symbol, range.start)
        cb.addJump(labelCond)
        cb.addLabel(labelStart)
        statements.forEach { it.codeGenStatement(cb, this) }
        val inc = cb.addAluOp(AluOp.ADD_I, symbol, makeSymbolIntLit(1), symbol.type)
        cb.addMov(symbol, inc)
        cb.addLabel(labelCond)
        cb.addBranch(braOp, labelStart, symbol, range.end)
    }
}