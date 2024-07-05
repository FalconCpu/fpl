class AstAssign(
    location: Location,
    private val lhs: Ast,
    private val rhs: Ast
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "ASSIGN\n")
        lhs.dump(sb,indent+1)
        rhs.dump(sb,indent+1)
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        val r = rhs.codeGenExpression(cb, context)
        lhs.codeGenLValue(cb, context, r)
    }
}