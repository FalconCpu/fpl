
/**
 * Generate the header for the assembly file
 */
//fun genAssemblyHeader(sb: StringBuilder) {
//    if (globalVariablesSize!=0)
//        sb.append("ld %29, _section_data:\n")
//    sb.append("jmp main\n")
//}


private fun InstrAlu.genAssembly() : String = when(op) {
    AluOp.NOP -> "nop"
    AluOp.ADD_I -> "add $dest, $a, $b"
    AluOp.SUB_I -> "sub $dest, $a, $b"
    AluOp.MUL_I -> "mul $dest, $a, $b"
    AluOp.DIV_I -> "divs $dest, $a, $b"
    AluOp.MOD_I -> "mods $dest, $a, $b"
    AluOp.AND_I -> "and $dest, $a, $b"
    AluOp.OR_I -> "or $dest, $a, $b"
    AluOp.XOR_I -> "xor $dest, $a, $b"
    AluOp.EQ_I -> "xor $dest, $a, $b\ncltu $dest, $dest, 1"
    AluOp.NE_I -> "xor $dest, $a, $b\ncltu $dest, 0, $dest"
    AluOp.LT_I -> "clt $dest, $a, $b"
    AluOp.GT_I -> "clt $dest, $b, $a"
    AluOp.LTE_I -> "clt $dest, $b, $a\nxor $dest, $dest, 1"
    AluOp.GTE_I -> "clt $dest, $a, $b\nxor $dest, $dest, 1"
    AluOp.LSL_I -> "lsl $dest, $a, $b"
    AluOp.LSR_I -> "lsr $dest, $a, $b"
    AluOp.ASR_I -> "asr $dest, $a, $b"
    AluOp.ADD_R -> TODO()
    AluOp.SUB_R -> TODO()
    AluOp.MUL_R -> TODO()
    AluOp.DIV_R -> TODO()
    AluOp.MOD_R -> TODO()
    AluOp.EQ_R -> TODO()
    AluOp.NE_R -> TODO()
    AluOp.LT_R -> TODO()
    AluOp.GT_R -> TODO()
    AluOp.LTE_R -> TODO()
    AluOp.GTE_R -> TODO()
    AluOp.ADD_S -> TODO()
    AluOp.EQ_S,
    AluOp.NE_S,
    AluOp.LT_S,
    AluOp.GT_S,
    AluOp.LTE_S,
    AluOp.GTE_S,
    AluOp.AND_B,
    AluOp.OR_B -> error("Should have been lowered before genAssembly")
}

private fun InstrBra.genAssembly() : String = when (op) {
    AluOp.EQ_I -> "beq $a, $b, .$label"
    AluOp.NE_I -> "bne $a, $b, .$label"
    AluOp.LT_I -> "blt $a, $b, .$label"
    AluOp.GT_I -> "blt $b, $a, .$label"
    AluOp.LTE_I -> "bge $b, $a, .$label"
    AluOp.GTE_I -> "bge $a, $b, .$label"
    else -> error("Not a valid branch operand")
}

private fun escape(str:String) : String {
    val sb = StringBuilder()
    for(c in str)
        if (c=='\n')
            sb.append("\\n")
        else
            sb.append(c)
    return sb.toString()
}

private fun Instr.genAssembly() = when(this) {
    is InstrAlu -> genAssembly()
    is InstrBra -> genAssembly()
    is InstrCall -> "jsr ${func.name}"
    is InstrCallReg -> "jsr $a[0]"
    is InstrChk -> error("Should have been lowered before genAssembly")
    is InstrEnd -> ""
    is InstrJmp -> "jmp .$label"
    is InstrLabel -> ".$label:"
    is InstrLea -> when (a) {
        is SymbolFunction -> "ld $dest, $a"
        is SymbolStringLit -> "ld $dest, \"${escape(a.name)}\""
        else -> error("Invalid type to LEA $a")
    }


    is InstrLoad -> {
        val op = if (size==1) "ldb" else if (size==2) "ldh" else if (size==4) "ldw" else error("Invalid size")
        val of = when (offset) {
            is SymbolMember -> offset.offset
            is SymbolGlobalVar -> offset.offset
            else -> offset.toString()
        }
        "$op $dest, $a[$of]"
    }
    is InstrMov -> "ld $dest, $a"
    is InstrNop -> "nop"
    is InstrStart -> ""
    is InstrStore -> {
        val op = if (size==1) "stb" else if (size==2) "sth" else if (size==4) "stw" else error("Invalid size")
        val of = when (offset) {
            is SymbolMember -> offset.offset
            is SymbolGlobalVar -> offset.offset
            else -> offset.toString()
        }
        "$op $data, $a[$of]"
    }
}



fun CodeBlock.genAssembly(sb:StringBuilder) {
    sb.append("$name:\n")

    // setup stack frame
    val makesCalls = prog.any{it is InstrCall || it is InstrCallReg }
    val stackSize = (if (maxRegister>8) 4*(maxRegister-8) else 0) + (if (makesCalls) 4 else 0)
    if (stackSize!=0) {
        sb.append("sub %sp, %sp, $stackSize\n")
        for(r in 9..maxRegister)
            sb.append("stw %$r, %sp[${4*(r-9)}]\n")
        if (makesCalls)
            sb.append("stw %30, %sp[${stackSize-4}]\n")
    }

    for(instr in prog) {
        if (instr is InstrStart || instr is InstrEnd)
            continue
        sb.append(instr.genAssembly())
        sb.append("\n")
    }

    // teardown stack frame
    if (stackSize!=0) {
        for(r in 9..maxRegister)
            sb.append("ldw %$r, %sp[${4*(r-9)}]\n")
        if (makesCalls)
            sb.append("ldw %30, %sp[${stackSize-4}]\n")
        sb.append("add %sp, %sp, $stackSize\n")
    }
    sb.append("ret\n")
}


// Strings have to be handled slightly differently to other data types
// as we leave their final locations up to the assembler. So maintain
// a map of which locations contain pointers to strings
private val stringLocations = mutableMapOf<Int,String>()

//fun genAssemblyGlobalVars(sb:StringBuilder) {
//    if (globalVariablesSize==0)
//        return
//
//    sb.append("_section_data:\n")
//
//    val globalMem = Array<Int>(globalVariablesSize/4){0}
//    stringLocations.clear()
//
//    for(v in allGlobalVariable)
//        v.initialValue?.simEval(globalMem, v.offset)
//
//    for(index in 0..<globalVariablesSize/4) {
//        val addr = index * 4
//        val label = allGlobalVariable.find {it.offset==addr}
//        if (label!=null)
//            sb.append("${label.name}: ")
//        val stringVal = stringLocations[addr]
//        if (stringVal!=null)
//            sb.append("dcw \"$stringVal\"\n")
//        else
//            sb.append("dcw ${globalMem[index]}\n")
//    }
//}
//
//fun Array<Int>.write(addr:Int, size:Int, value:Int) {
//    val a = addr / 4;
//    val shift = (addr and 3) *8
//    when(size) {
//        1-> {
//            // write byte
//            val mask = 0xff shl shift
//            this[a] = (this[a] and mask.inv()) or ((value and 0xff) shl shift)
//        }
//
//        2-> {
//            // write half word
//            assert(addr % 2 == 0)
//            val mask = 0xffff shl shift
//            this[a] = (this[a] and mask.inv()) or ((value and 0xffff) shl shift)
//        }
//
//        4-> {
//            // write whole word
//            assert(addr % 4 == 0)
//            this[a] = value
//        }
//
//        else -> error("Invalid write size")
//    }
//}

