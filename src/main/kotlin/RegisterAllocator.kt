
class RegisterAllocator(private val cb: CodeBlock, private val livemap: Livemap) {

    // reg numbers  0..31 represent the CPU registers,
    // CPU register number 0, 29,30 and 31 have dedicated uses. leaving numbers 1..28 for allocation
    // reg numbers 32 and up are the user variables that need to be assigned to cpu registers.
    // Register numbers 1..8 potentially get clobbered by function calls
    // Register number -1 is used to indicate currently unallocated
    private val all_cpu_regs = 0..31
    private val cpu_regs = 1..28
    private val num_vars = cb.symbols.size
    private val user_vars = 32..<num_vars
    private val UNALLOCATED = -1
    private val caller_save_regs = 1..8


    // Array of which registers are allocated to each variable
    private val alloc = Array(num_vars){ if (it<=31) it else UNALLOCATED}

    // Array of which Args interfere with each arg
    private val interfere = Array(num_vars){mutableSetOf<Int>() }

    // List of MOV statements in the prog, where both operands are variables
    private val movStatements = cb.prog.filterIsInstance<InstrMov>().filter{it.a.isVar() && it.dest.isVar()}

    private fun Symbol.isVar() = this is SymbolLocalVar  || this is SymbolReg || this is SymbolTemp

    private val debug = false


    /**
     * Build a map listing every Arg that interferes with an arg
     */
    private fun buildInterfere() {
        for (instr in cb.prog) {
            // Args written by an instruction interfere with everything live at that point
            // (Except for a MOV statement - no interference is created between its Dest and Src)
            if (instr is InstrData)
                for (liveIndex in livemap.live[instr.index + 1].stream()) {
                    if (liveIndex != instr.dest.index && !(instr is InstrMov && liveIndex == instr.a.index)) {
                        interfere[instr.dest.index] += liveIndex
                        interfere[liveIndex] += instr.dest.index
                    }
                }


            // A call statement could potentially clobber registers %1-%8, so mark those
            if (instr is InstrCall || instr is InstrCallReg) {
                for (liveIndex in livemap.live[instr.index + 1].stream()) {
                    for (dest in caller_save_regs)
                        if (liveIndex != dest) {
                            interfere[dest] += liveIndex
                            interfere[liveIndex] += dest
                        }
                }
            }
        }
    }

    private fun dumpInterfere() {
        println("Interfere Graph:")
        for(index in interfere.indices)
            if (interfere[index].isNotEmpty())
                println("${cb.symbols[index]} = ${interfere[index].joinToString { cb.symbols[it].name }}")
    }

    /**
     * Assign variable 'v' to register 'r'
     */

    private fun assign(v:Int, r:Int) {
        if (debug)
            println("Assigning ${cb.symbols[v]} to ${cb.symbols[r]}")
        assert(r in all_cpu_regs)
        assert(v in user_vars)
        assert(! interfere[r].contains(v))

        alloc[v] = r
        interfere[r] += interfere[v]
        if (r>cb.maxRegister && r<=28)
            cb.maxRegister = r
    }

    private fun lookForCoalesce() {
        do {
            var again = false
            for (mov in movStatements) {
                val a = mov.a.index
                val d = mov.dest.index
                if (alloc[a] == UNALLOCATED && alloc[d] != UNALLOCATED && (a !in interfere[alloc[d]])) {
                    assign(a, alloc[d])
                    again = true
                }
                if (alloc[d] == UNALLOCATED && alloc[a] != UNALLOCATED && (d !in interfere[alloc[a]])) {
                    assign(d, alloc[a])
                    again = true
                }
            }
        } while (again)
    }

    /**
     * Find a register which does not have an interference with 'v'
     */
    private fun findAssignFor(v:Int) : Int{
        for (r in cpu_regs) {
            if (v !in interfere[r])
                return r
        }
        error("Unable to find a register for ${cb.symbols[v]}")
    }

    private fun replace(a: Symbol) = if (a.isVar()) cb.symbols[alloc[a.index]] else a

    private fun Instr.replaceVars() : Instr {
        val new = when(this) {
            is InstrAlu -> InstrAlu(op, replace(dest), replace(a), replace(b))
            is InstrBra -> InstrBra(op, label, replace(a), replace(b))
            is InstrCall -> this
            is InstrCallReg -> InstrCallReg(replace(a))
            is InstrChk -> InstrChk(replace(a), replace(bounds))
            is InstrEnd -> this
            is InstrJmp -> this
            is InstrLabel -> this
            is InstrLea -> InstrLea(replace(dest), replace(a))
            is InstrLoad -> InstrLoad(size, replace(dest), replace(a), replace(offset))
            is InstrMov -> InstrMov(replace(dest), replace(a))
            is InstrNop -> this
            is InstrStart -> this
            is InstrStore -> InstrStore(size, replace(data), replace(a), replace(offset))
        }
        new.index = index
        return new
    }

    fun run() {
        if (debug)
            livemap.dump()
        buildInterfere()
        if (debug)
            dumpInterfere()

        // Perform the allocation starting with the most difficult vars
        val vars = user_vars.sortedByDescending { interfere[it].size }
        lookForCoalesce()

        for(v in vars) {
            if (alloc[v] == UNALLOCATED) {
                val r = findAssignFor(v)
                assign(v, r)
                lookForCoalesce()
            }
        }

        for(index in cb.prog.indices)
            cb.prog[index] = cb.prog[index].replaceVars()
    }
}