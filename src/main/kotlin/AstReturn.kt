class AstReturn (
    location: Location,
    private val value: Ast?
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "RETURN\n")
        value?.dump(sb, indent + 1)
    }

    override fun codeGenStatement(cb: CodeBlock, context: AstBlock) {
        val enclosingFunction = context.findEnclosingFunction()
        if (enclosingFunction == null) {
            Log.error(location, "Cannot return from top-level code")
            return
        }

        if (value==null) {
            if (enclosingFunction.retType != TypeUnit)
                Log.error(location, "Function should return '${enclosingFunction.retType}'")
        } else {
            val v = value.codeGenExpression(cb, context)
            if (!enclosingFunction.retType.isTypeCompatible(v))
                Log.error(location, "Function should return '${enclosingFunction.retType}', not '${v.type}'")
            cb.add( InstrMov(cb.getReg(8), v))
        }
        cb.add( InstrJmp(enclosingFunction.endLabel))
    }
}