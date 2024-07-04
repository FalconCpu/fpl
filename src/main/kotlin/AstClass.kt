class AstClass (
    location: Location,
    parent : AstBlock,
    private val name: String,
    private val astParams: List<AstParam>,
    private val astSuperClass : Ast?
) : AstBlock(location, parent) {
    lateinit var type : TypeClass
    lateinit var params: List<Symbol>            // Parameters for the constructor
    private val codeBlock = newCodeBlock(name)   // Code block for the constructor
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
        codeBlock.add (InstrStart())

        thisSym = SymbolLocalVar("this", type, false)
        codeBlock.add(thisSym)
        add(location,thisSym)
        codeBlock.add(InstrMov(thisSym, codeBlock.getReg(1)))

        val params = mutableListOf<Symbol>()
        for ((index, param) in astParams.withIndex()) {
            val symbol = param.createMemberSymbol(context)
            params += symbol
            add(param.location, symbol)
            if (symbol is SymbolLocalVar) {
                codeBlock.add(symbol)
                codeBlock.add(InstrMov(symbol, codeBlock.getReg(index + 2)))
            } else {
                codeBlock.add(InstrStore(symbol.type.getSize(), codeBlock.getReg(index + 2), thisSym, symbol))
                type.members += symbol
            }
        }

        for (statement in statements) {
            if (statement is AstFunction)
                statement.identifyFunctions(this)
            else if (statement is AstDecl)
                statement.codeGenMember(codeBlock, this)
        }

        codeBlock.add(InstrEnd(emptyList()))
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        for (statement in statements)
            if (statement is AstFunction)
                statement.codeGenStatement(codeBlock, this)
    }
}