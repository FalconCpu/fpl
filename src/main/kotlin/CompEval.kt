
fun compEval(op: AluOp, lhs: SymbolIntLit, rhs:SymbolIntLit) : SymbolIntLit {
    val l = lhs.value
    val r = rhs.value

    val v = when (op) {
        AluOp.NOP -> 0
        AluOp.ADD_I -> l + r
        AluOp.SUB_I -> l - r
        AluOp.MUL_I -> l * r
        AluOp.DIV_I -> l / r
        AluOp.MOD_I -> l % r
        AluOp.AND_I -> l and r
        AluOp.OR_I -> l or r
        AluOp.XOR_I -> l xor r
        AluOp.EQ_I -> if (l == r) 1 else 0
        AluOp.NE_I -> if (l != r) 1 else 0
        AluOp.LT_I -> if (l < r) 1 else 0
        AluOp.GT_I -> if (l > r) 1 else 0
        AluOp.LTE_I -> if (l <= r) 1 else 0
        AluOp.GTE_I -> if (l >= r) 1 else 0
        AluOp.LSL_I -> l shl r
        AluOp.LSR_I -> l ushr r
        AluOp.ASR_I -> l shr r
        else -> error("Unsupported op")
    }
    return makeSymbolIntLit(v)
}