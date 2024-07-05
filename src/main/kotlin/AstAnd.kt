class AstAnd (
    location: Location,
    val lhs: Ast,
    val rhs: Ast
) : Ast(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "AND\n")
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
        lhs.codeGenBranch(cb, context, labMid, labFalse)

        cb.addLabel(labMid)
        val midFalse = cb.pathStateFalse
        cb.pathState = cb.pathStateTrue
        cb.pathStateFalse = cb.pathStateTrue
        rhs.codeGenBranch(cb, context, labTrue, labFalse)
        cb.pathStateFalse = joinState(listOf(cb.pathStateFalse, midFalse))
    }
}