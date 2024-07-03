class AstFuncType (
    location: Location,
    private val params: List<Ast>,
    private val retType: Ast
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "FUNC\n")
        params.forEach { it.dump(sb, indent + 1) }
        retType.dump(sb, indent + 1)
    }

    override fun resolveType(context: AstBlock): Type {
        val p = params.map { it.resolveType(context) }
        val r = retType.resolveType(context)
        return makeTypeFunction(p, r)
    }
}