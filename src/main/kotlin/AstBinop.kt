class AstBinop(
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
        val l = lhs.codeGenExpression(cb, context)
        val r = rhs.codeGenExpression(cb, context)
        if (l is SymbolError) return l
        if (r is SymbolError) return r

        val op = binopTable.find{ it.kind == kind && it.lhs == l.type && it.rhs == r.type }
        if (op==null)
            return makeSymbolError(location, "No operation defined for ${l.type} $kind ${r.type}")

        if (l is SymbolIntLit && r is SymbolIntLit)
            return compEval(op.outOp, l, r)

        val ret = cb.newTemp(op.outType)
        cb.add( InstrAlu(op.outOp, ret, l, r))
        return ret
    }

    override fun codeGenBranch(cb: CodeBlock, context: AstBlock, labTrue: Label, labFalse: Label) {
        when (kind) {
            TokenKind.EQ,
            TokenKind.NEQ,
            TokenKind.LT,
            TokenKind.LTE,
            TokenKind.GT,
            TokenKind.GTE -> {
                val l = lhs.codeGenExpression(cb, context)
                val r = rhs.codeGenExpression(cb, context)
                if (l is SymbolError || r is SymbolError)
                    return
                val op = binopTable.find{ it.kind == kind && it.lhs == l.type && it.rhs == r.type }
                if (op==null) {
                    Log.error(location, "No operation defined for ${l.type} $kind ${r.type}")
                    return
                }
                cb.add( InstrBra(op.outOp, labTrue, l, r))
                cb.add( InstrJmp(labFalse))
            }

            TokenKind.AND -> TODO()
            TokenKind.OR -> TODO()

            else -> {
                super.codeGenBranch(cb, context, labTrue, labFalse)
            }
        }
    }
}


// ---------------------------------------------------------------------------------
//                          Binary Operators
// ---------------------------------------------------------------------------------

private class Binop (val kind: TokenKind, val lhs: Type, val rhs: Type, val outOp: AluOp, val outType: Type)
private val binopTable = listOf(
    Binop(TokenKind.PLUS,     TypeInt, TypeInt, AluOp.ADD_I, TypeInt),
    Binop(TokenKind.MINUS,    TypeInt, TypeInt, AluOp.SUB_I, TypeInt),
    Binop(TokenKind.STAR,     TypeInt, TypeInt, AluOp.MUL_I, TypeInt),
    Binop(TokenKind.SLASH,    TypeInt, TypeInt, AluOp.DIV_I, TypeInt),
    Binop(TokenKind.PERCENT,  TypeInt, TypeInt, AluOp.MOD_I, TypeInt),
    Binop(TokenKind.AMPERSAND, TypeInt, TypeInt, AluOp.AND_I, TypeInt),
    Binop(TokenKind.BAR,      TypeInt, TypeInt, AluOp.OR_I,  TypeInt),
    Binop(TokenKind.CARAT,    TypeInt, TypeInt, AluOp.XOR_I, TypeInt),
    Binop(TokenKind.LEFT,     TypeInt, TypeInt, AluOp.LSL_I, TypeInt),
    Binop(TokenKind.RIGHT,    TypeInt, TypeInt, AluOp.ASR_I, TypeInt),
    Binop(TokenKind.EQ,       TypeInt, TypeInt, AluOp.EQ_I,  TypeBool),
    Binop(TokenKind.NEQ,      TypeInt, TypeInt, AluOp.NE_I,  TypeBool),
    Binop(TokenKind.LT,       TypeInt, TypeInt, AluOp.LT_I,  TypeBool),
    Binop(TokenKind.LTE,      TypeInt, TypeInt, AluOp.LTE_I, TypeBool),
    Binop(TokenKind.GT,       TypeInt, TypeInt, AluOp.GT_I,  TypeBool),
    Binop(TokenKind.GTE,      TypeInt, TypeInt, AluOp.GTE_I, TypeBool),

    Binop(TokenKind.PLUS,     TypeReal, TypeReal, AluOp.ADD_R, TypeReal),
    Binop(TokenKind.MINUS,    TypeReal, TypeReal, AluOp.SUB_R, TypeReal),
    Binop(TokenKind.STAR,     TypeReal, TypeReal, AluOp.MUL_R, TypeReal),
    Binop(TokenKind.SLASH,    TypeReal, TypeReal, AluOp.DIV_R, TypeReal),
    Binop(TokenKind.PERCENT,  TypeReal, TypeReal, AluOp.MOD_R, TypeReal),
    Binop(TokenKind.EQ,       TypeReal, TypeReal, AluOp.EQ_R,  TypeBool),
    Binop(TokenKind.NEQ,      TypeReal, TypeReal, AluOp.NE_R,  TypeBool),
    Binop(TokenKind.LT,       TypeReal, TypeReal, AluOp.LT_R,  TypeBool),
    Binop(TokenKind.LTE,      TypeReal, TypeReal, AluOp.LTE_R, TypeBool),
    Binop(TokenKind.GT,       TypeReal, TypeReal, AluOp.GT_R,  TypeBool),
    Binop(TokenKind.GTE,      TypeReal, TypeReal, AluOp.GTE_R, TypeBool),

    Binop(TokenKind.PLUS,     TypeString, TypeString, AluOp.ADD_S, TypeString),
    Binop(TokenKind.EQ,       TypeString, TypeString, AluOp.EQ_S,  TypeBool),
    Binop(TokenKind.NEQ,      TypeString, TypeString, AluOp.NE_S,  TypeBool),
    Binop(TokenKind.LT,       TypeString, TypeString, AluOp.LT_S,  TypeBool),
    Binop(TokenKind.LTE,      TypeString, TypeString, AluOp.LTE_S, TypeBool),
    Binop(TokenKind.GT,       TypeString, TypeString, AluOp.GT_S,  TypeBool),
    Binop(TokenKind.GTE,      TypeString, TypeString, AluOp.GTE_S, TypeBool),

    Binop(TokenKind.AND,      TypeBool,  TypeBool, AluOp.AND_B, TypeBool),
    Binop(TokenKind.OR,       TypeBool,  TypeBool, AluOp.OR_B,  TypeBool)
)

