class AstCharLit (
    location: Location,
    val text : String
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "CHARLIT $text\n")
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        return makeSymbolIntLit(text[0].code, TypeChar)
    }
}