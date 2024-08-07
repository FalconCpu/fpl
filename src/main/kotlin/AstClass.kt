class AstClass (
    location: Location,
    parent : AstBlock,
    private val name: String,
    private val astParams: List<AstParam>,
    private val astSuperClass : Ast?
) : AstBlock(location, parent) {
    lateinit var type : TypeClass
    val constructorParams = mutableListOf<Symbol>()   // Parameters for the constructor
    val codeBlock = newCodeBlock(name)   // Code block for the constructor
    lateinit var thisSym : SymbolLocalVar

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "CLASS $name\n")
        astParams.forEach { it.dump(sb, indent + 1) }
        statements.forEach { it.dump(sb, indent + 1) }
    }

    override fun add(statement: Ast) {
        if (statement !is AstFunction && statement !is AstDecl) {
            Log.error(statement.location, "Invalid statement in class")
        }
        statements += statement
    }

    fun identifyClass(context: AstBlock) {
        type = makeTypeClass(name, this)
        val typeSym = SymbolTypeName(name, type)
        context.add(location, typeSym)
    }

    fun identifyMembers(context: AstBlock) {
        thisSym = SymbolLocalVar("this", type, false)
        codeBlock.add(thisSym)
        add(location,thisSym)
        codeBlock.addMov(thisSym, codeBlock.getReg(1))

        for ((index, param) in astParams.withIndex()) {
            val symbol = param.createMemberSymbol(context)
            constructorParams += symbol
            add(param.location, symbol)
            if (symbol is SymbolLocalVar) {
                codeBlock.add(symbol)
                codeBlock.addMov(symbol, codeBlock.getReg(index + 2))
            } else {
                codeBlock.addStore(symbol.type, codeBlock.getReg(index + 2), thisSym, symbol)
                type.members += symbol
            }
        }

        for (statement in statements) {
            if (statement is AstFunction)
                statement.identifyFunctions(this)
            else if (statement is AstDecl)
                statement.codeGenMember(codeBlock, this)
        }

        codeBlock.addEnd(emptyList())
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        for (statement in statements)
            if (statement is AstFunction)
                statement.codeGenStatement(codeBlock, this)
    }
}