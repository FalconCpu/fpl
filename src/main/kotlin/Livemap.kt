
import java.util.BitSet

class Livemap(private val cb: CodeBlock) {
    private val numRows = cb.prog.size
    private val numCols = cb.symbols.size

    val live = Array(numRows){BitSet(numCols)}
    private val kill = Array(numRows){BitSet(numCols)}

    private fun Symbol.isVar() = this is SymbolLocalVar || this is SymbolReg || this is SymbolTemp

    private fun gen() {
        for(instr in cb.prog) {
            if (instr is InstrData)
                kill[instr.index][instr.dest.index]=true
            instr.getUse().filter { it.isVar() }.forEach{ live[instr.index][it.index]=true}
        }
    }

    private fun propagate() {
        var madeChanges : Boolean
        do {
            madeChanges = false
            for (instr in cb.prog.asReversed()) {
                when (instr) {
                    is InstrEnd -> {}
                    is InstrJmp -> {
                        val count = live[instr.index].cardinality()
                        live[instr.index].or(live[instr.label.index])
                        if (live[instr.index].cardinality() > count)
                            madeChanges=true
                    }

                    is InstrBra -> {
                        val count = live[instr.index].cardinality()
                        live[instr.index].or(live[instr.label.index])
                        live[instr.index].or(live[instr.index + 1])
                        if (live[instr.index].cardinality() > count)
                            madeChanges=true
                    }

                    else -> {
                        // Find the bits live at next instruction that are not killed by this one
                        val x = live[instr.index + 1].clone() as BitSet
                        x.andNot(kill[instr.index])
                        live[instr.index].or(x)
                    }
                }
            }
        } while(madeChanges)
    }

    fun dump() {

        for(y in 0..4) {
            print(" ".repeat(30))
            for (x in 0..<numCols) {
                print(cb.symbols[x].name.padStart(5)[y])
                if (x % 8 == 7)
                    print(' ')
            }
            print("\n")
        }

        for(y in 0..<numRows) {
            print("%-30s".format(cb.prog[y].toString()))
            for (x in 0..<numCols) {
                val l = live[y][x]
                val k = kill[y][x]
                val c = if (l && k) 'B' else if (l) 'X' else if (k) 'K' else '.'
                print(c)
                if (x%8==7)
                    print(' ')
            }
            print("\n")
        }
    }

    init {
        gen()
        propagate()
    }
}