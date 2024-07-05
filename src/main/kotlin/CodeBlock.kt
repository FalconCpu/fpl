val allCodeBlocks = mutableListOf<CodeBlock>()

fun newCodeBlock(name: String): CodeBlock {
    val codeBlock = CodeBlock(name)
    allCodeBlocks += codeBlock
    return codeBlock
}

class CodeBlock(val name:String) {
    val prog = mutableListOf<Instr>(InstrStart())
    val symbols = allSymbolReg.toMutableList<Symbol>()
    val labels = mutableListOf<Label>()
    var hasLocalStackVars = false  // Used to track if we need to allocate a frame pointer

    // During the ast tree walk - keep track of what is known about the state of variables
    var pathState = PathState(emptySet(), emptySet(), emptyMap(), false)
    var pathStateTrue = pathState
    var pathStateFalse = pathState

    private var numTemps = 0
    var maxRegister = 1
    private val tempVars = mutableMapOf<Expression, SymbolTemp>()
    private var tempCount = 0

    private fun add(instr: Instr) {
        prog += instr
    }

    fun add(symbol: Symbol) {
        if (! symbols.contains(symbol))
            symbols += symbol
    }

    fun newLabel(): Label {
        val label = Label("@" + labels.size)
        labels += label
        return label
    }

    fun newTemp(type:Type, expression: Expression): SymbolTemp {
        val ret = tempVars.getOrPut(expression) {
            val temp = SymbolTemp("&" + numTemps++, type, expression)
            symbols += temp
            temp
        }
        check(ret.type == type)
        return ret
    }

    fun newTempNoExpr(type:Type) : SymbolTemp {
        val expr = Expression(AluOp.NOP, makeSymbolIntLit(tempCount++), symbolZero)
        return newTemp(type, expr)
    }

    fun addAluOp(op: AluOp, lhs:Symbol, rhs:Symbol, type: Type) : Symbol {
        val expression = Expression(op, lhs, rhs)
        val dest = newTemp(type, expression)
        add(InstrAlu(op, dest, lhs, rhs))
        return dest
    }

    fun addLoad(type:Type, addr:Symbol, offset:Symbol) : Symbol {
        val op = when(type.getSize()) {
            1 -> AluOp.B
            2 -> AluOp.H
            4 -> AluOp.W
            else -> error("Invalid size $type")
        }
        val expression = Expression(op, addr, offset)
        val dest = newTemp(type, expression)
        add(InstrLoad(op, dest, addr, offset))
        return dest
    }

    fun addStore(type:Type, value:Symbol, addr:Symbol, offset:Symbol) {
        val op = when(type.getSize()) {
            1 -> AluOp.B
            2 -> AluOp.H
            4 -> AluOp.W
            else -> error("Invalid size $type")
        }
        val expression = Expression(op, addr, offset)
        val dest = newTemp(type, expression)
        add(InstrMov(dest,value))
        add(InstrStore(op, dest, addr, offset))
    }

    fun addCopy(value:Symbol, type: Type) : Symbol {
        val dest = newTempNoExpr(type)
        add(InstrMov(dest, value))
        return dest
    }

    fun addLea(type: Type, value: Symbol) : Symbol {
        val expr = Expression(AluOp.NOP, value, symbolZero)
        val dest = newTemp(type,expr)
        add(InstrLea(dest, value))
        return dest
    }


    fun addMov(dest:Symbol, value:Symbol) : Symbol {
        add(InstrMov(dest, value))
        return dest
    }

    fun addLabel(label:Label) {
        add(InstrLabel(label))
    }

    fun addEnd(retVal:List<Symbol>) {
        add(InstrEnd(retVal))
    }

    fun addJump(label: Label) {
        add(InstrJmp(label))
    }

    fun addBranch(op: AluOp, label: Label, lhs: Symbol, rhs: Symbol) {
        add(InstrBra(op, label, lhs, rhs))
    }

    fun addCall(func:CodeBlock) {
        add(InstrCall(func))
    }

    fun addCallR(func:Symbol) {
        add(InstrCallReg(func))
    }


    fun dump(sb: StringBuilder) {
        sb.append("*****************************************************\n")
        sb.append("              $name\n")
        sb.append("*****************************************************\n")
        prog.forEach{ sb.append(it.toString() + "\n") }
        sb.append("\n")
    }

    fun dumpWithLineNumbers(): String {
        val sb = StringBuilder()
        sb.append("*****************************************************\n")
        sb.append("              $name\n")
        sb.append("*****************************************************\n")
        prog.forEachIndexed{ index, instr ->  sb.append("%3d %s\n".format(index, instr.toString())) }
        sb.append("\n")
        return sb.toString()
    }

    fun getReg(index:Int) = allSymbolReg[index]

    fun getThis() =
        symbols.find { it.name == "this" } ?: error("No 'this' symbol found")
}