class AstConst (
    location: Location,
    val name: String,
    val astType : Ast?,
    val value: Ast
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "CONST $name\n")
        value.dump(sb, indent + 1)
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        val v = value.codeGenExpression(cb, context)
        if (v !is SymbolIntLit && v !is SymbolStringLit) {
            Log.error(location, "Constant value must be a string or integer literal")
            return
        }

        val type = astType?.resolveType(context) ?: v.type
        if (!type.isTypeCompatible(v))
            Log.error(location, "Constant value of type ${v.type} is not compatible with declared type ${type}")

        val symbol = when(v) {
            is SymbolIntLit -> SymbolIntLit(name, type, v.value)
            is SymbolStringLit -> SymbolStringLit(name, type, v.value)
            else -> error("Invalid type of value")
        }

        context.add(location, symbol)
    }
}