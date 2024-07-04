class AstDecl (
    location: Location,
    private val kind : TokenKind,
    private val name : String,
    private val type : Ast?,
    private val init : Ast?
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "$kind $name\n")
        type?.dump(sb, indent+1)
        init?.dump(sb, indent+1)
    }

    override fun codeGenStatement(cb: CodeBlock, context:AstBlock) {
        val rhs = init?.codeGenExpression(cb, context)
        val type = type?.resolveType(context) ?: rhs?.type ?: makeTypeError(location,"Cannot determine type of $name")
        val mutable = kind == TokenKind.VAR
        val symbol = if (context is AstTop)
            SymbolGlobalVar(name, type, mutable)
        else
            SymbolLocalVar(name, type, mutable)
        context.add(location, symbol)
        cb.add(symbol)
        if (rhs != null) {
            if (!symbol.type.isTypeCompatible(rhs))
                Log.error(location, "Cannot assign value of type ${rhs.type} to variable of type ${symbol.type}")
            cb.addMov( symbol, rhs)
        }
    }

    fun codeGenMember(cb: CodeBlock, context: AstClass) {
        val rhs = init?.codeGenExpression(cb, context)
        val type = type?.resolveType(context) ?: rhs?.type ?: makeTypeError(location, "Cannot determine type of $name")
        val mutable = kind == TokenKind.VAR
        val symbol = SymbolMember(name, type, mutable)
        context.add(location, symbol)
        context.type.members += symbol
        if (rhs != null) {
            if (!symbol.type.isTypeCompatible(rhs))
                Log.error(location, "Cannot assign value of type ${rhs.type} to variable of type ${symbol.type}")
            cb.addStore(rhs.type, rhs, context.thisSym, symbol)
        }
    }
}