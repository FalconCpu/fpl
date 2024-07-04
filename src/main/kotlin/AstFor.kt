class AstFor (
    location: Location,
    parent : AstBlock,
    val id: String,
    val astFrom: Ast,
    val astTo: Ast,
    val excludeEnd : Boolean
) : AstBlock(location,parent) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "FOR $id\n")
        astFrom.dump(sb, indent + 1)
        astTo.dump(sb, indent + 1)
        statements.forEach { it.dump(sb, indent + 1) }
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        val startVal = astFrom.codeGenExpression(cb, context)
        val endVal = astTo.codeGenExpression(cb, context)
        if (!TypeInt.isTypeCompatible(startVal))
            Log.error(location, "FROM value must be of type int")
        if (!startVal.type.isTypeCompatible(endVal))
            Log.error(location, "FROM and TO values must be of the same type")

        val symbol = SymbolLocalVar(id, startVal.type, false)
        add(location, symbol)
        cb.add(symbol)

        val labelCond = cb.newLabel()
        val labelStart = cb.newLabel()
        val braOp = if (excludeEnd) AluOp.LT_I else AluOp.LTE_I
        cb.addMov(symbol, startVal)
        cb.addJump(labelCond)
        cb.addLabel(labelStart)
        statements.forEach { it.codeGenStatement(cb, this) }
        val inc = cb.addAluOp(AluOp.ADD_I, symbol, makeSymbolIntLit(1), symbol.type)
        cb.addMov(symbol, inc)
        cb.addLabel(labelCond)
        cb.addBranch(braOp, labelStart, symbol, endVal)
    }
}