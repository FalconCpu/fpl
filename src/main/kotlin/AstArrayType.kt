class AstArrayType (
    location: Location,
    private val base: Ast,
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "[]\n")
        base.dump(sb, indent + 1)
    }

    override fun resolveType(context: AstBlock): Type {
        val b = base.resolveType(context)
        return makeTypeArray(b)
    }
}