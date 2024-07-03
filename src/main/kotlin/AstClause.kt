class AstClause (
    location: Location,
    parent : AstBlock,
    val condition: Ast?,
) : AstBlock(location, parent) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "CLAUSE\n")
        condition?.dump(sb, indent + 1)
        statements.forEach { it.dump(sb, indent + 1) }
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        statements.forEach { it.codeGenStatement(cb, this) }
    }
}