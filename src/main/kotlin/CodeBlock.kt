val allCodeBlocks = mutableListOf<CodeBlock>()

fun newCodeBlock(name: String): CodeBlock {
    val codeBlock = CodeBlock(name)
    allCodeBlocks += codeBlock
    return codeBlock
}

class CodeBlock(val name:String) {
    val prog = mutableListOf<Instr>()
    val symbols = allSymbolReg.toMutableList<Symbol>()
    val labels = mutableListOf<Label>()
    private var numTemps = 0
    var maxRegister = 1

    fun add(instr: Instr) {
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

    fun newTemp(type:Type): SymbolTemp {
        val temp = SymbolTemp( "&" + numTemps++, type)
        symbols += temp
        return temp
    }

    fun addAluOp(op: AluOp, lhs:Symbol, rhs:Symbol, type: Type=lhs.type) : Symbol {
        val dest = newTemp(type)
        add(InstrAlu(op, dest, lhs, rhs))
        return dest
    }

    fun addNewTemp(src: Symbol, type:Type=src.type) : Symbol {
        val dest = newTemp(type)
        add(InstrMov(dest, src))
        return dest
    }

    fun addLabel(label:Label) {
        add(InstrLabel(label))
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