class AstParam(
    location: Location,
    private val kind : TokenKind,
    private val name : String,
    private val type : Ast
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "PARAM $name\n")
        type.dump(sb,indent+1)
    }

    fun createSymbol(context:AstBlock): Symbol {
        val mutable = kind == TokenKind.VAR
        return SymbolLocalVar(name, type.resolveType(context), mutable)
    }

    fun createMemberSymbol(context:AstBlock): Symbol {
        val type = type.resolveType(context)
        return when(kind) {
            TokenKind.VAL -> SymbolMember(name, type, false)
            TokenKind.VAR -> SymbolMember(name, type, true)
            TokenKind.EOF -> SymbolLocalVar(name, type, false)
            else -> error("Invalid kind $kind")
        }
    }

}