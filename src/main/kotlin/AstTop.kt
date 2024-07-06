class AstTop(
    location: Location,
) : AstBlock(location, null) {
    val codeBlock = newCodeBlock("TopLevel")

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "TOP\n")
        statements.forEach{it.dump(sb, indent+1)}
    }

    fun dump(): String {
        val sb = StringBuilder()
        dump(sb,0)
        return sb.toString()
    }

    override fun add(statement: Ast) {
        when(statement) {
            is AstDecl,
            is AstFunction,
            is AstClass,
            is AstConst,
            is AstEnum -> statements += statement
            else ->   Log.error(statement.location, "Invalid statement in top level")
        }
    }

    fun generateIR() {
        for (statement in statements)
            statement.codeGenStatement(codeBlock, this)
        codeBlock.addEnd(emptyList())
    }

    fun dumpIR(): String {
        val sb = StringBuilder()
        for(codeBlock in allCodeBlocks)
            codeBlock.dump(sb)
        return sb.toString()
    }

    fun identifyFunctions() {
        for (statement in statements)
            if (statement is AstFunction)
                statement.identifyFunctions(this)
            else if (statement is AstClass)
                statement.identifyMembers(this)
    }

    fun identifyClasses() {
        for (statement in statements)
            if (statement is AstClass)
                statement.identifyClass(this)
            else if (statement is AstEnum)
                statement.identifyEnum(this)
    }
}