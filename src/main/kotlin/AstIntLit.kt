class AstIntLit (
    location: Location,
    val text : String
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "INTLIT $text\n")
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        return try {
            if (text.endsWith('H', ignoreCase = true))
                makeSymbolIntLit(text.dropLast(1).toLong(16).toInt(), TypeInt)
            else
                makeSymbolIntLit(text.toInt(), TypeInt)
        } catch (e: NumberFormatException) {
            makeSymbolError(location, "Invalid integer literal")
        }
    }
}