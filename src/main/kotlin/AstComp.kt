class AstComp(
    location: Location,
    private val kind:TokenKind,
    private val lhs: Ast,
    private val rhs: Ast
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "$kind\n")
        lhs.dump(sb,indent+1)
        rhs.dump(sb,indent+1)
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        TODO()
    }

    private fun kindToOp(kind: TokenKind) = when(kind) {
        TokenKind.EQ -> AluOp.EQ_I
        TokenKind.NEQ -> AluOp.NE_I
        TokenKind.LT -> AluOp.LT_I
        TokenKind.LTE -> AluOp.LTE_I
        TokenKind.GT -> AluOp.GT_I
        TokenKind.GTE -> AluOp.GTE_I
        else -> error("Invalid comparison")
    }

    override fun codeGenBranch(cb: CodeBlock, context: AstBlock, labTrue: Label, labFalse: Label) {
        val l = lhs.codeGenExpression(cb, context)
        val r = rhs.codeGenExpression(cb, context)
        val op = kindToOp(kind)

        if (l is SymbolError || r is SymbolError)
            return
        if (!l.type.isTypeCompatible(r))
            return Log.error(location, "Incompatible types for comparison: ${l.type} $kind ${r.type}")
        if (l.type is TypeString || l.type is TypeReal)
            TODO("Strings and reals comparison not yet implemented")

        if (op==AluOp.EQ_I && l.type is TypeNullable && r.type is TypeNull) {
            cb.pathStateTrue = cb.pathState.addSmartCast(l, TypeNull)
            cb.pathStateFalse = cb.pathState.addSmartCast(l, l.type.base)
        }
        if (op==AluOp.NE_I && l.type is TypeNullable && r.type is TypeNull) {
            cb.pathStateFalse = cb.pathState.addSmartCast(l, TypeNull)
            cb.pathStateTrue = cb.pathState.addSmartCast(l, l.type.base)
        }
        if (op == AluOp.EQ_I && l.type is TypeNull && r.type is TypeNullable) {
            cb.pathStateTrue = cb.pathState.addSmartCast(r, TypeNull)
            cb.pathStateFalse = cb.pathState.addSmartCast(r, r.type.base)
        }
        if (op == AluOp.NE_I && l.type is TypeNull && r.type is TypeNullable) {
            cb.pathStateFalse = cb.pathState.addSmartCast(r, TypeNull)
            cb.pathStateTrue = cb.pathState.addSmartCast(r, r.type.base)
        }

        cb.addBranch( op, labTrue, l, r)
        cb.addJump( labFalse)
    }
}
