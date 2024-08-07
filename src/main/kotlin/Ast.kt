abstract class Ast (val location: Location) {
    abstract fun dump(sb:StringBuilder, indent:Int)

    open fun codeGenStatement(cb:CodeBlock, context:AstBlock) {
        error("${this.javaClass} not supported in code generation as statement")
    }

    open fun codeGenExpression(cb: CodeBlock, context:AstBlock) : Symbol {
        error("${this.javaClass} not supported in code generation as expression")
    }

    open fun codeGenExpressionOrTypeName(cb: CodeBlock, context: AstBlock): Symbol {
        return codeGenExpression(cb,context)
    }

    open fun codeGenLValue(cb: CodeBlock, context:AstBlock, value:Symbol) {
        error("${this.javaClass} not supported in code generation as lvalue")
    }

    open fun codeGenBranch(cb: CodeBlock, context:AstBlock, labTrue:Label, labFalse:Label) {
        val r = codeGenExpression(cb, context)
        if (!TypeBool.isTypeCompatible(r))
            Log.error(location, "Condition must be of type bool not '${r.type}'")
        cb.addBranch( AluOp.NE_I, labTrue, r, symbolZero)
        cb.addJump( labFalse)
    }

    open fun resolveType(context: AstBlock): Type {
        error("${this.javaClass} not supported in type resolution")
    }
}