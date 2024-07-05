class AstQmark(
    location: Location,
    private val lhs: Ast
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "QMARK\n")
        lhs.dump(sb, indent + 1)
    }

    override fun resolveType(context:AstBlock): Type {
        val ret = lhs.resolveType(context)
        if (ret is TypeError) return ret
        if (ret !is TypeClass && ret !is TypeString && ret !is TypeArray)
            return makeTypeError(location, "QMARK can only be applied to a class, string or array")
        return makeTypeNullable(ret)
    }
}