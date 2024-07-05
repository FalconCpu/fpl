import java.util.*

class AvailableMap(val cb:CodeBlock) {
    // Build a map indicating which temp vars are still valid at a given point in the program

    val allTemps = cb.symbols.filterIsInstance<SymbolTemp>()
    val numRows = cb.prog.size
    val numCols = cb.symbols.size
    val availMap = Array(numRows) { BitSet(numCols) }

    val debug = true

    private fun genKillMap() {
        for (i in 0..<numRows)
            availMap[i].set(0,numCols)
        for(temp in allTemps)
            availMap[0][temp.index] = false

        for (instr in cb.prog) {
            when(instr) {
                is InstrData -> {
                    if (instr.dest.index==31) // Don't include the stack pointer in the kill map
                        continue
                    for (temp in allTemps) {
                        if (temp.dependsOn(instr.dest))
                            availMap[instr.index + 1][temp.index] = false
                    }
                }

                is InstrStore ->
                    // For now assume a store instruction kills all memory expressions
                    // TODO - make this a bit more precise - add some more checks to determine if the store
                    // could possibly affect the value of the memory expression
                    for (temp in allTemps)
                        if (temp.expression.op==AluOp.B || temp.expression.op==AluOp.H || temp.expression.op==AluOp.W)
                            availMap[instr.index+1][temp.index] = false

                is InstrCall, is InstrCallReg ->
                    // For now assume a call kills all memory expressions
                    for (temp in allTemps)
                        if (temp.expression.op==AluOp.B || temp.expression.op==AluOp.H || temp.expression.op==AluOp.W)
                            availMap[instr.index+1][temp.index] = false

                else -> {}
            }
        }
    }

    private fun propagate() {
        var madeChanges: Boolean
        do {
            madeChanges = false
            for (instr in cb.prog) {
                if (instr !is InstrJmp && instr !is InstrEnd)
                    availMap[instr.index + 1].and(availMap[instr.index])

                when (instr) {
                    is InstrEnd -> {}
                    is InstrJmp -> {
                        val old = availMap[instr.label.index].clone() as BitSet
                        availMap[instr.label.index].and(availMap[instr.index])
                        madeChanges = madeChanges || availMap[instr.label.index] != old
                    }

                    is InstrBra -> availMap[instr.label.index].and(availMap[instr.index])
                    is InstrData -> availMap[instr.index + 1].set(instr.dest.index)
                    else -> {}
                }
            }
        } while(madeChanges)
    }

    fun dump() {

        for(temp in allTemps)
            println("${temp.name}  ${temp.expression}")

        for(y in 0..4) {
            print(" ".repeat(33))
            for (x in 0..<numCols) {
                print(cb.symbols[x].name.padStart(5)[y])
                if (x % 8 == 7)
                    print(' ')
            }
            print("\n")
        }

        for(y in 0..<numRows) {
            print("%2d %-30s".format(y,cb.prog[y].toString()))
            for (x in 0..<numCols) {
                val l = availMap[y][x]
                val c = if (l) 'X' else ' '
                print(c)
                if (x%8==7)
                    print(' ')
            }
            print("\n")
        }
    }

    fun generate(): Array<BitSet> {
        genKillMap()
        propagate()
        if (debug)
            dump()
        return availMap
    }
}