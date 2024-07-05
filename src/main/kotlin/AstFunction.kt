class AstFunction (
    location: Location,
    parent : AstBlock,
    val name : String,
    private val astParams : List<AstParam>,
    private val astRetType : Ast?,
) : AstBlock(location, parent){
    val codeBlock = newCodeBlock(name)
    lateinit var params: List<Symbol>
    lateinit var retType: Type
    val endLabel = codeBlock.newLabel()

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "FUNC $name\n")
        astParams.forEach{it.dump(sb, indent+1)}
        astRetType?.dump(sb,indent+1)
        statements.forEach{it.dump(sb,indent+1)}
    }

    override fun codeGenStatement(cb: CodeBlock, context:AstBlock) {
        for(statement in statements)
            statement.codeGenStatement(codeBlock, this)
        codeBlock.addLabel(endLabel)
        val retval = if (retType is TypeUnit) emptyList() else listOf(codeBlock.getReg(8))
        codeBlock.addEnd(retval)
    }

    fun identifyFunctions(context: AstBlock) {
        retType = astRetType?.resolveType(context) ?: TypeUnit
        val params = mutableListOf<Symbol>()
        for((index,param) in astParams.withIndex()) {
            val symbol = param.createSymbol(context)
            params += symbol
            add(param.location, symbol)
            codeBlock.add(symbol)
            codeBlock.addMov(symbol, codeBlock.getReg(index+1))
        }

        this.params = params

        val funcType = makeTypeFunction(params.map { it.type }, retType)
        val funcSym = SymbolFunction(name, funcType, this)
        context.add(location, funcSym)
    }

}