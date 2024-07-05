class AstOr (
    location: Location,
    val lhs: Ast,
    val rhs: Ast
) : Ast(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "OR\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        TODO()
    }

    override fun codeGenBranch(cb: CodeBlock, context: AstBlock, labTrue: Label, labFalse: Label) {
        val labMid = cb.newLabel()

        cb.pathStateTrue = cb.pathState
        cb.pathStateFalse = cb.pathState
        lhs.codeGenBranch(cb, context, labTrue, labMid)

        cb.addLabel(labMid)
        val midTrue = cb.pathStateTrue
        cb.pathState = cb.pathStateFalse
        cb.pathStateTrue = cb.pathStateFalse
        rhs.codeGenBranch(cb, context, labTrue, labFalse)
        cb.pathStateTrue = joinState(listOf(cb.pathStateFalse, midTrue))
    }
}