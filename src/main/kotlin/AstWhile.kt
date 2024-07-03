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
        val labCond = cb.newLabel()
        val labEnd = cb.newLabel()
        val labStart = cb.newLabel()
        cb.add( InstrJmp(labCond))
        cb.add( InstrLabel(labStart))
        statements.forEach { it.codeGenStatement(cb, this) }
        cb.add( InstrLabel(labCond))
        cond.codeGenBranch(cb, context, labStart, labEnd)
        cb.add( InstrLabel(labEnd))
    }
}