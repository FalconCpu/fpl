class AstStringLit (
    location: Location,
    val text : String
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "STRLIT $text\n")
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val symbolStringLit = makeSymbolStringLit(text)
        return cb.addLea(TypeString, symbolStringLit)
    }
}