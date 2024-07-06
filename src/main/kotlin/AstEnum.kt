class AstEnum (
    location: Location,
    val name: String,
    val entries: List<AstId>
) : Ast(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "ENUM $name\n")
        entries.forEach { it.dump(sb, indent + 1) }
    }

    fun identifyEnum(context:AstBlock) {
        val enumType = TypeEnum(name)
        for((index,entry) in entries.withIndex()) {
            val sym = SymbolIntLit(entry.name, enumType, index)
            if (enumType.members.any { it.name == sym.name })
                Log.error(entry.location, "Duplicate enum entry ${entry.name}")
            enumType.members += sym
        }

        val enumSym = SymbolTypeName(name, enumType)
        context.add(location, enumSym)
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        // Do nothing
    }

}