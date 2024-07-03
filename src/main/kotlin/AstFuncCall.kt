class AstFuncCall(
    location: Location,
    private val lhs: Ast,
    private val rhs: List<Ast>
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "FuncCall\n")
        lhs.dump(sb,indent+1)
        rhs.forEach{it.dump(sb,indent+1)}
    }


    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val funcSym = lhs.codeGenExpression(cb, context)
        if (funcSym.type !is TypeFunction)
            return makeSymbolError(location, "Cannot call non-function")
        val args = rhs.map { it.codeGenExpression(cb, context) }

        val params = funcSym.type.params
        if (args.size != params.size)
            return makeSymbolError(location, "Function expects ${params.size} arguments, got ${args.size}")
        for(index in args.indices) {
            if (!params[index].isTypeCompatible(args[index])) {
                Log.error(
                    rhs[index].location,
                    "Argument ${index+1}: Got type ${args[index].type} when expecting ${params[index]}"
                )
            }
            cb.add(InstrMov(cb.getReg(index+1), args[index]))
        }

        if (funcSym is SymbolFunction)
            cb.add(InstrCall(funcSym.function))
        else
            cb.add(InstrCallReg(funcSym))

        if (funcSym.type.retType is TypeUnit) {
            return cb.getReg(0)
        } else {
            val ret = cb.newTemp(funcSym.type.retType)
            cb.add(InstrMov(ret, cb.getReg(8)))
            return ret
        }
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        codeGenExpression(cb, context)
    }
}