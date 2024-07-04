





private fun Instr.legalize(cb: CodeBlock) : Instr {

    fun legalizeArg(symbol: Symbol) : Symbol {
        // For most instructions the arg cannot be an int literal, unless it is 0
        if (symbol !is SymbolIntLit || symbol.value == 0)
            return symbol
        return cb.addCopy(symbol, symbol.type)
    }

    fun legalizeLit(symbol: Symbol) : Symbol {
        // For some instructions the arg can be a literal in the range -0x1000 to 0xfff
        if (symbol !is SymbolIntLit || symbol.value in -0x1000..0xfff)
            return symbol
        return cb.addCopy(symbol, symbol.type)
    }

    return when (this) {
        is InstrAlu -> InstrAlu(op, dest, legalizeArg(a), legalizeLit(b))
        is InstrBra -> InstrBra(op, label, legalizeArg(a), legalizeArg(b))
        is InstrStore -> InstrStore(size, legalizeArg(data), legalizeArg(a), offset)
        is InstrLoad -> InstrLoad(size, dest, legalizeArg(a), offset)
        is InstrCall,
        is InstrCallReg,
        is InstrChk,
        is InstrLea,
        is InstrMov,
        is InstrEnd,
        is InstrJmp,
        is InstrLabel,
        is InstrNop,
        is InstrStart -> this
    }
}

fun CodeBlock.legalize() {
    val inProg = prog.toList()
    prog.clear()
    for (instr in inProg)
        prog += instr.legalize(this)
}
