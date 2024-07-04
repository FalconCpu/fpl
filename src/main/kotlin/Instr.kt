
sealed class Instr {
    var index = 0

    override fun toString(): String {
        return when (this) {
            is InstrAlu -> "$op $dest, $a, $b"
            is InstrBra -> "B$op $a, $b, $label"
            is InstrCall -> "CALL ${func.name}"
            is InstrCallReg -> "CALLR $a"
            is InstrEnd -> "END"
            is InstrJmp -> "JMP $label"
            is InstrLabel -> "$label:"
            is InstrLoad -> "LD$size $dest, $a[$offset]"
            is InstrMov -> "MOV $dest, $a"
            is InstrNop -> "NOP"
            is InstrLea -> "LEA $dest, $a"
            is InstrChk -> "CHK $a, $bounds"
            is InstrStart -> "START"
            is InstrStore -> "ST$size $data, $a[$offset]"
        }
    }
}

sealed class InstrData(val dest:Symbol) : Instr()
sealed class InstrFlow(val label: Label) : Instr()

class InstrNop : Instr()
class InstrMov(dest: Symbol, val a: Symbol) : InstrData(dest)
class InstrAlu(val op: AluOp, dest: Symbol, val a: Symbol, val b: Symbol) : InstrData(dest)
class InstrBra(val op: AluOp, label: Label,val a: Symbol, val b: Symbol) : InstrFlow(label)
class InstrJmp(label: Label) : InstrFlow(label)
class InstrLabel(label: Label) : InstrFlow(label)
class InstrLoad(val size:AluOp, dest: Symbol, val a: Symbol, val offset: Symbol) : InstrData(dest)
class InstrStore(val size:AluOp, val data: Symbol, val a: Symbol, val offset: Symbol) : Instr()
class InstrCall(val func: AstFunction) : Instr()
class InstrCallReg(val a: Symbol) : Instr()
class InstrLea(dest: Symbol, val a: Symbol) : InstrData(dest)
class InstrChk(val a: Symbol, val bounds: Symbol) : Instr()
class InstrStart : Instr()
class InstrEnd(val retval:List<Symbol>) : Instr()

enum class AluOp {
    NOP,
    ADD_I,
    SUB_I,
    MUL_I,
    DIV_I,
    MOD_I,
    AND_I,
    OR_I,
    XOR_I,
    EQ_I,
    NE_I,
    LT_I,
    GT_I,
    LTE_I,
    GTE_I,
    LSL_I,
    LSR_I,
    ASR_I,

    ADD_R,
    SUB_R,
    MUL_R,
    DIV_R,
    MOD_R,
    EQ_R,
    NE_R,
    LT_R,
    GT_R,
    LTE_R,
    GTE_R,

    ADD_S,
    EQ_S,
    NE_S,
    LT_S,
    GT_S,
    LTE_S,
    GTE_S,

    AND_B,
    OR_B,

    B,   // Memory ops
    H,
    W,

    MOV
}


/**
 * get a list of all symbols read by an instruction
 */
fun Instr.getUse() : List<Symbol> = when(this) {
    is InstrAlu -> listOf(a,b)
    is InstrLea -> listOf(a)
    is InstrLoad -> listOf(a, offset)
    is InstrMov -> listOf(a)
    is InstrBra -> listOf(a,b)
    is InstrCall -> emptyList()
    is InstrCallReg -> listOf(a)
    is InstrEnd -> retval
    is InstrJmp -> emptyList()
    is InstrLabel -> emptyList()
    is InstrNop -> emptyList()
    is InstrStart -> emptyList()
    is InstrChk -> listOf(a, bounds)
    is InstrStore -> listOf(data, offset, a)
}

fun AluOp.isCommutative() : Boolean = when (this) {
    AluOp.ADD_I,
    AluOp.SUB_I,
    AluOp.MUL_I,
    AluOp.AND_I,
    AluOp.OR_I,
    AluOp.XOR_I,
    AluOp.EQ_I,
    AluOp.NE_I -> true
    else -> false
}
