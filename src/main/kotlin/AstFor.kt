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
        val fromVal = astFrom.codeGenExpression(cb, context)
        val endVal = astTo.codeGenExpression(cb, context)
        if (!TypeInt.isTypeCompatible(fromVal))
            Log.error(location, "FROM value must be of type int")
        if (!fromVal.type.isTypeCompatible(endVal))
            Log.error(location, "FROM and TO values must be of the same type")

        val symbol = SymbolLocalVar(id, fromVal.type, false)
        add(location, symbol)
        cb.add(symbol)

        val labelCond = cb.newLabel()
        val labelStart = cb.newLabel()
        val braOp = if (excludeEnd) AluOp.LT_I else AluOp.LTE_I
        cb.add(InstrMov(symbol, fromVal))
        cb.add(InstrJmp(labelCond))
        cb.add(InstrLabel(labelStart))
        statements.forEach { it.codeGenStatement(cb, this) }
        cb.add(InstrAlu(AluOp.ADD_I, symbol, symbol, makeSymbolIntLit(1)))
        cb.add(InstrLabel(labelCond))
        cb.add(InstrBra(braOp, labelStart, symbol, endVal))
    }
}