import java.util.*

class AvailableMap(val cb:CodeBlock) {
    // Build a map indicating which temp vars are still valid at a given point in the program

    val allTemps = cb.symbols.filterIsInstance<SymbolTemp>()
    val numRows = cb.prog.size
    val numCols = cb.symbols.size
    val availMap = Array(numRows) { BitSet(numCols) }
    val symOtherMemory = SymbolReg("MEMORY")

    val debug = false

    private fun Expression.isMemory() = this.op==AluOp.W || this.op==AluOp.H || this.op==AluOp.B

    private fun buildDependenceMap() {
        for (temp in allTemps)
            temp.dep.clear()

        var madeChange : Boolean
        do {
            madeChange = false
            for (instr in cb.prog) {
                if (instr is InstrData && instr.dest is SymbolTemp) {
                    val count = instr.dest.dep.size
                    for (sym in instr.getUse()) {
                        instr.dest.dep += sym
                        if (sym is SymbolTemp)
                            instr.dest.dep += sym.dep
                    }
                    // A load instruction from a calculated address is considered as a dependancy on general memory
                    if (instr is InstrLoad && instr.offset !is SymbolMember && instr.offset !is SymbolGlobalVar)
                        instr.dest.dep += symOtherMemory
                    if (instr.dest.dep.size > count)
                        madeChange = true
                }
            }
        } while (madeChange)

        if (debug) {
            for (temp in allTemps) {
                println("$temp: ${temp.dep}")
            }
        }
    }

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
                        if (temp.dep.contains(instr.dest))
                            availMap[instr.index + 1][temp.index] = false
                    }
                }

                is InstrStore -> {
                    val dest = if (instr.offset is SymbolMember || instr.offset is SymbolGlobalVar)
                        instr.offset else symOtherMemory
                    for (temp in allTemps)
                        if (temp.dep.contains(dest))
                            availMap[instr.index + 1][temp.index] = false
                }

                is InstrCall, is InstrCallReg ->
                    // Assume a function call potentially modifies all mutable variables and fields
                    for (temp in allTemps) {
                        if (temp.dep.contains(symOtherMemory) ||
                            temp.dep.any  {it is SymbolMember && it.mutable } ||
                            temp.dep.any  {it is SymbolGlobalVar && it.mutable } )
                            availMap[instr.index + 1][temp.index] = false
                    }

                else -> {}
            }
        }
    }

    private fun propagate() {

        if (debug) {
            println("Before propagation:")
            dump()
        }
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

                    is InstrBra -> {
                        val old = availMap[instr.label.index].clone() as BitSet
                        availMap[instr.label.index].and(availMap[instr.index])
                        madeChanges = madeChanges || availMap[instr.label.index] != old
                    }

                    is InstrData -> availMap[instr.index + 1].set(instr.dest.index)

                    is InstrStore ->
                        if (instr.data is SymbolTemp)
                            availMap[instr.index + 1].set(instr.data.index)

                    else -> {}
                }
            }
        } while(madeChanges)
    }

    fun dump() {

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
        buildDependenceMap()
        genKillMap()
        propagate()
        if (debug)
            dump()
        return availMap
    }
}