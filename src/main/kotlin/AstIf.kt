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
        val clauseStates = mutableListOf<PathState>()
        val endLabel = cb.newLabel()

        // Generate code for each clause condition, and remember the label and state for each one
        for(clause in clauses) {
            if (clause.condition != null) {
                val nextClauseLabel = cb.newLabel()
                val thisLabel = cb.newLabel()
                clauseLabels += thisLabel
                cb.pathStateTrue = cb.pathState
                cb.pathStateFalse = cb.pathState
                clause.condition.codeGenBranch(cb, context, thisLabel, nextClauseLabel)
                clauseStates += cb.pathStateTrue
                cb.pathState = cb.pathStateFalse
                cb.addLabel(nextClauseLabel)
            } else {
                clauseStates += cb.pathState
                val thisLabel = cb.newLabel()
                clauseLabels += thisLabel
                cb.addJump(thisLabel)
            }
        }

        val outStates = mutableListOf<PathState>()
        if (clauses.none { it.condition == null }) {
            // If there is no else clause, then allow for fall-through
            outStates += cb.pathState
            cb.addJump(endLabel)
        }

        // Now generate the code the body of each clause
        for((index,clause) in clauses.withIndex()) {
            cb.addLabel( clauseLabels[index])
            cb.pathState = clauseStates[index]
            clause.codeGenStatement(cb, context)
            outStates += cb.pathState
            cb.addJump(endLabel)
        }

        cb.addLabel(endLabel)
        cb.pathState = joinState(outStates)
    }
}