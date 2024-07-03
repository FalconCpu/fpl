class AstIf(
    location: Location,
    private val clauses: List<AstClause>
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "IF\n")
        clauses.forEach { it.dump(sb, indent + 1) }
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        val clauseLabels = mutableListOf<Label>()
        val endLabel = cb.newLabel()
        for(clause in clauses) {
            if (clause.condition != null) {
                val nextClauseLabel = cb.newLabel()
                val thisLabel = cb.newLabel()
                clauseLabels += thisLabel
                clause.condition.codeGenBranch(cb, context, thisLabel, nextClauseLabel)
                cb.add(InstrLabel(nextClauseLabel))
            } else {
                clause.codeGenStatement(cb, context)
            }
        }
        cb.add(InstrJmp(endLabel))

        for((index,clause) in clauses.withIndex()) {
            if (clause.condition == null)
                break
            cb.add( InstrLabel(clauseLabels[index]))
            clause.codeGenStatement(cb, context)
            cb.add(InstrJmp(endLabel))
        }

        cb.add(InstrLabel(endLabel))
    }
}