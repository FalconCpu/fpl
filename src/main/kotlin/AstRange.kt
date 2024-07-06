class AstRange (
    location: Location,
    val start: Ast,
    val end: Ast,
    val inclusive: Boolean
) : Ast(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "RANGE\n")
        start.dump(sb, indent + 1)
        end.dump(sb, indent + 1)
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val startSymbol = start.codeGenExpression(cb, context)
        val endSymbol = end.codeGenExpression(cb, context)
        if (startSymbol.type != endSymbol.type)
            return makeSymbolError(location, "Start and end of Range must be of the same type")
        if (!startSymbol.type.isEnumerable())
            return makeSymbolError(location, "Start and end of Range must be of an enumerable type")
        val rangeType = makeTypeRange(startSymbol.type, inclusive)
        val name = if (inclusive) "$startSymbol..$endSymbol" else "$startSymbol..<$endSymbol"
        return SymbolRange(name, rangeType, startSymbol, endSymbol)
    }
}