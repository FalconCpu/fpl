class AstRepeat (
    location: Location,
    parent: AstBlock,
) : AstBlock(location, parent) {
    lateinit var cond: Ast

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "REPEAT\n")
        cond.dump(sb, indent + 1)
        statements.forEach{it.dump(sb, indent + 1) }
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        val labStart = cb.newLabel()
        val labEnd = cb.newLabel()
        cb.addLabel( labStart)
        statements.forEach { it.codeGenStatement(cb, this) }

        cb.pathStateTrue = cb.pathState
        cb.pathStateFalse = cb.pathState
        cond.codeGenBranch(cb, context, labEnd, labStart)
        cb.addLabel( labEnd)
        cb.pathState = cb.pathStateFalse
    }
}