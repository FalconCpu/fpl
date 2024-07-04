class AstWhile (
    location: Location,
    parent: AstBlock,
    private val cond : Ast
) : AstBlock(location,parent){

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "WHILE\n")
        cond.dump(sb,indent+1)
        statements.forEach{it.dump(sb,indent+1)}
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        val labStart = cb.newLabel()
        val labBody = cb.newLabel()
        val labEnd = cb.newLabel()
        cb.addLabel( labStart)
        cb.pathStateTrue = cb.pathState
        cb.pathStateFalse = cb.pathState
        cond.codeGenBranch(cb, context, labBody, labEnd)
        cb.pathState = cb.pathStateTrue
        val pathStateAtEnd = cb.pathStateFalse
        cb.addLabel( labBody)
        statements.forEach { it.codeGenStatement(cb, this) }
        cb.addJump(labStart)
        cb.addLabel( labEnd)
        cb.pathState = pathStateAtEnd
    }
}