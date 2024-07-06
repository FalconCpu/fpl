import AluOp.*
import java.util.*

class Peephole(private val cb: CodeBlock) {
    private val prog = cb.prog

    private val debug = false
    private var madeChange = false
    private lateinit var availableMap : Array<BitSet>

    /**
     * Tests to see if an Arg is of a kind that can be stored in a register
     */
    private fun Symbol.isVar() = this is SymbolLocalVar || this is SymbolReg || this is SymbolTemp

    /**
     * Tests to see if an Arg is assigned to exactly one, and if so returns the Instr
     */
    private fun Symbol.getSSA() : InstrData? {
        if (!isVar())
            return null
        if (def.size==1)
            return def[0]
        return null
    }

    /**
     * Re-build indexes of the program.
     *
     * Remove useless instructions / Variables / Labels
     * Sets every Var and Instr to have the correct index
     * Sets index of Labels to point to the instruction index where they are defined
     * Build def- and use- information for all Vars
     */

    private fun rebuildExpressions() {
        for (instr  in cb.prog) {
            if (instr is InstrData && instr.dest is SymbolTemp) {
                when(instr) {
                    is InstrAlu -> instr.dest.expression = Expression(instr.op, instr.a, instr.b)
                    is InstrLea -> {}
                    is InstrLoad ->instr.dest.expression = Expression(instr.size, instr.a, instr.offset)
                    is InstrMov -> instr.dest.expression = Expression(MOV, instr.a, symbolZero)
                }
            }
        }
    }

    private fun rebuildIndex() {
        prog.removeIf { it is InstrNop || it is InstrMov && it.dest==it.a}

        for(v in cb.symbols) {
            v.def.clear()
            v.use.clear()
        }
        for(l in cb.labels) {
            l.index = -1
            l.use.clear()
        }

        for((index,instr) in prog.withIndex()) {
            instr.index = index
            if (instr is InstrData)
                instr.dest.def += instr
            instr.getUse().filter{ it.isVar() } .forEach{ it.use += instr}
            if (instr is InstrLabel)
                instr.label.index = index
            if (instr is InstrBra)
                instr.label.use += instr
            if (instr is InstrJmp)
                instr.label.use += instr
        }

        cb.symbols.removeIf { (it is SymbolTemp || it is SymbolLocalVar) && it.def.isEmpty() && it.use.isEmpty()}
        cb.labels.removeIf { it.index==-1 }

        for((index,v) in cb.symbols.withIndex())
            v.index = index
    }

    private fun changeToNop(instr: Instr) = changeToNop(instr.index)

    private fun changeToNop(index:Int) {
        val ins = prog[index]
        if (ins is InstrNop)
            return

        if (debug)
            println("Removing $index")

        if (ins is InstrData)
            ins.dest.def.remove(ins)
        for(u in prog[index].getUse())
            u.use.remove(ins)
        prog[index] = InstrNop()
        madeChange = true
    }

    private fun Instr.replaceWith(newInstr: Instr) {
        if (debug)
            println("Replaced $index with $newInstr")
        newInstr.index = index
        if (this is InstrData)
            dest.def.remove(this)
        for(u in getUse())
            u.use.remove(this)
        if (newInstr is InstrData)
            newInstr.dest.def += newInstr
        for(u in newInstr.getUse())
            if (u.isVar())
                u.use += newInstr
        prog[index] = newInstr
        madeChange = true
    }

    private fun isSmallInt(v: SymbolIntLit?) : Boolean {
        if (v==null)
            return false
        return v.value>=-0x1000 && v.value<0xFFF
    }

    /**
     * Invert a branch operation
     */
    private fun AluOp.invert() : AluOp = when(this) {
        EQ_I  -> NE_I
        NE_I  -> EQ_I
        LT_I  -> GTE_I
        GT_I  -> LTE_I
        LTE_I -> GT_I
        GTE_I -> LT_I
        else -> error("malformed branch instruction")
    }

    /**
     * Check to see if a Variable has a constant value - if so return it
     */
    private fun Symbol.varHasConstantValue() : SymbolIntLit? {
        if (this is SymbolIntLit)
            return this

        if ((this is SymbolTemp || this is SymbolLocalVar) && def.size == 1) {
            val ssa = def[0]
            if (ssa is InstrMov)
                return if (ssa.a is SymbolIntLit)
                    ssa.a
                else
                    ssa.a.varHasConstantValue()
        }
        return null
    }

    private fun Int.isPowerOf2() = (this and (this-1))==0

    private fun log2(x:Int) : Int {
        var ret = 0
        var n = x
        while (n>1) {
            ret++
            n /= 2
        }
        return ret
    }

    private fun InstrMov.peephole() {
        // remove instructions when the result is never used
        if (dest.use.isEmpty() && dest !is SymbolReg)
            return changeToNop(this)

        if (dest==a)
            return changeToNop(this)

        val aConst = a.varHasConstantValue()
        if (a.isVar() && aConst!=null)
            return replaceWith( InstrMov(dest, aConst))
    }

    private fun InstrAlu.peephole() {
        // remove instructions when the result is never used
        if (dest.use.isEmpty() && dest !is SymbolReg)
            return changeToNop(this)

        val aConst = a.varHasConstantValue()
        val bConst = b.varHasConstantValue()

        if (aConst!=null && bConst!=null) {
            val ev = compEval(op, aConst,bConst)
            return replaceWith( InstrMov(dest, ev))
        }

        if (aConst!=null && a.isVar() && isSmallInt(aConst) && op.isCommutative())
            return replaceWith( InstrAlu(op, dest, b, aConst) )

        if (bConst!=null) {
            if (b.isVar() && isSmallInt(bConst))
                return replaceWith(InstrAlu(op, dest, a, bConst))

            if (bConst.value == 0 && (op == ADD_I || op == SUB_I || op == OR_I || op == XOR_I || op == LSL_I || op == LSR_I))
                return replaceWith(InstrMov(dest, a))

            if (bConst.value == 0 && (op == AND_I || op == MUL_I))
                return replaceWith(InstrMov(dest, symbolZero))

            if (bConst.value == 1 && (op == MUL_I))
                return replaceWith(InstrMov(dest, a))

            if (bConst.value.isPowerOf2() && op == MUL_I)
                return replaceWith(InstrAlu(LSL_I, dest, a, makeSymbolIntLit(log2(bConst.value))))

            if (bConst.value.isPowerOf2() && op == DIV_I)
                return replaceWith(InstrAlu(ASR_I, dest, a, makeSymbolIntLit(log2(bConst.value))))

            if (bConst.value.isPowerOf2() && op == MOD_I && bConst.value<=0x1000)
                return replaceWith(InstrAlu(AND_I, dest, a, makeSymbolIntLit(bConst.value-1)))
        }
    }

    private fun InstrJmp.peephole() {
        // Look for jumps to next instruction
        if (label.index==index+1)
            changeToNop(this)
    }

    private fun InstrBra.peephole() {
        // Look for branch to next instruction
        if (label.index==index+1)
            changeToNop(this)

        // look for a branch to a label that is immediately followed by a jump
        val instAtBranchDest = prog[label.index+1]
        if ( instAtBranchDest is InstrJmp)
            replaceWith( InstrBra( op, instAtBranchDest.label, a, b))

        // Look for either of the arguments = 0
        val aConst = a.varHasConstantValue()
        val bConst = b.varHasConstantValue()
        if (a.isVar() && aConst!=null && aConst.value==0)
            return replaceWith( InstrBra( op, label, symbolZero, b))
        if (b.isVar() && bConst!=null && bConst.value==0)
            return replaceWith( InstrBra( op, label, a, symbolZero))

        // Look for a branch over a jump.
        val nextInstr = prog[index+1]
        if (label.index==index+2 && nextInstr is InstrJmp) {
            replaceWith( InstrBra( op.invert(), nextInstr.label, a, b))
            changeToNop( index+1 )
        }
    }

    private fun InstrLabel.peephole() {
        // labels that are never used
        if (label.use.isEmpty())
            changeToNop(this)
    }

    private fun InstrStore.peephole() {
        // Look for stores of value zero
        if (data.isVar()) {
            val dataConst = data.varHasConstantValue()
            if (dataConst != null && dataConst.value == 0)
                replaceWith(InstrStore(size, symbolZero, a, offset))
        }
    }

    /**
     * Finds instruction specific peephole optimizations.
     */

    private fun Instr.peephole()  {
        // dispatch to specific peephole functions
        when (this) {
            is InstrMov -> peephole()
            is InstrAlu -> peephole()
            is InstrJmp -> peephole()
            is InstrBra -> peephole()
            is InstrLabel -> peephole()
            is InstrStore -> peephole()
            else -> {}
        }
    }

    private fun InstrAlu.checkCse() {
        // sanity check - make sure any temps used in the instruction are available
        if (a is SymbolTemp)
            check(availableMap[index][a.index])
        if (b is SymbolTemp)
            check(availableMap[index][b.index])

        if (dest is SymbolTemp && availableMap[index][dest.index]) {
            if (debug)
                println("CSE: $this")
            changeToNop(index)
        }
    }

    private fun InstrMov.checkCse() {
        // sanity check - make sure any temps used in the instruction are available
        if (a is SymbolTemp)
            check(availableMap[index][a.index])

        if (dest is SymbolTemp && availableMap[index][dest.index]) {
            if (debug)
                println("CSE: $this")
            changeToNop(index)
        }
    }

    private fun InstrStore.checkCse() {
        // sanity check - make sure any temps used in the instruction are available
        if (a is SymbolTemp)
            check(availableMap[index][a.index])

        // Look to see if the source comes from an add to literal
        if (a is SymbolTemp && offset is SymbolIntLit) {
            val aExpr = a.expression
            if (aExpr.op==ADD_I && aExpr.rhs is SymbolIntLit)
                replaceWith(InstrStore(size, data, a.expression.lhs, makeSymbolIntLit(offset.value + aExpr.rhs.value)))
        }
    }

    private fun InstrLoad.checkCse() {
        // sanity check - make sure any temps used in the instruction are available
        if (a is SymbolTemp)
            check(availableMap[index][a.index])

        if (dest is SymbolTemp && availableMap[index][dest.index]) {
            if (debug)
                println("CSE: $this")
            changeToNop(index)
        }


        // Look to see if the source comes from an add to literal
        if (a is SymbolTemp && offset is SymbolIntLit) {
            val aExpr = a.expression
            if (aExpr.op==ADD_I && aExpr.rhs is SymbolIntLit)
                replaceWith(InstrLoad(size, dest, a.expression.lhs, makeSymbolIntLit(offset.value + aExpr.rhs.value)))
        }
    }

    private fun commonSubexpressionPass() {
        for(instr in prog) {
            when(instr) {
                is InstrMov -> instr.checkCse()
                is InstrAlu -> instr.checkCse()
                is InstrStore -> instr.checkCse()
                is InstrLoad -> instr.checkCse()
                else -> {}
            }
        }
    }

    /**
     * Make a pass through the program, removing unreachable instructions
     * and calling the peephole function on others
     */

    private fun peepholePass() {
        var reachable = true
        for(instr in prog) {
            if (instr  is InstrLabel)
                reachable = true
            if (reachable)
                instr.peephole()
            else
                changeToNop(instr)
            if (instr is InstrJmp)
                reachable = false
        }
    }



    fun run() {
        var passNumber = 0
        do {
            madeChange = false
            rebuildIndex()
            if (debug)
                println(cb.dumpWithLineNumbers())
            peepholePass()

            if (passNumber>=1) {
                rebuildExpressions()
                rebuildIndex()
                availableMap = AvailableMap(cb).generate()
                commonSubexpressionPass()
            }

            passNumber++
        } while(passNumber==1 || madeChange && passNumber < 10)
    }
}
