class AstCast (
    location: Location,
    private val expr: Ast,
    private val type: Ast
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "CAST\n")
        expr.dump(sb, indent + 1)
        type.dump(sb, indent + 1)
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val exprSymbol = expr.codeGenExpression(cb, context)
        val typeSymbol = type.resolveType(context)
        return cb.addNewTemp(exprSymbol, typeSymbol)
    }
}