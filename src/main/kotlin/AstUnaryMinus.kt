class AstUnaryMinus (
    location: Location,
    val expr: Ast
) : Ast(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "UNARY MINUS\n")
        expr.dump(sb, indent + 1)
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val exprSymbol = expr.codeGenExpression(cb, context)
        if (exprSymbol.type !is TypeInt)
            return makeSymbolError(location, "Unary minus can only be applied to an integer")
        return cb.addAluOp(AluOp.SUB_I, symbolZero, exprSymbol, TypeInt)
    }
}