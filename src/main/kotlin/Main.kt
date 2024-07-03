
fun main() {

}

enum class StopAt {AST, IR, PEEPHOLE, REGALLOC}

fun compile( files:List<Lexer>, stopAt: StopAt) : String {

    Log.setTestMode()
    allCodeBlocks.clear()

    val top = AstTop(nullLocation)
    for(file in files)
        Parser(file).parseTop(top)

    if (Log.allErrors.isNotEmpty())
        return Log.allErrors.joinToString(separator = "\n")
    if (stopAt==StopAt.AST)
        return top.dump()

    top.identifyClasses()
    top.identifyFunctions()
    top.generateIR()

    if (Log.allErrors.isNotEmpty())
        return Log.allErrors.joinToString(separator = "\n")
    if (stopAt==StopAt.IR)
        return top.dumpIR()

    allCodeBlocks.forEach { Peephole(it).run() }
    if (stopAt==StopAt.PEEPHOLE)
        return top.dumpIR()

    for(cb in allCodeBlocks) {
        val livemap = Livemap(cb)
        RegisterAllocator(cb, livemap).run()
        Peephole(cb).run()
    }
    if (Log.allErrors.isNotEmpty())
        return Log.allErrors.joinToString(separator = "\n")
    if (stopAt==StopAt.REGALLOC)
        return top.dumpIR()


    TODO()
}