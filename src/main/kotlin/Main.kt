import java.io.File
import java.util.concurrent.TimeUnit

fun main() {

}

enum class StopAt {AST, IR, PEEPHOLE, REGALLOC, ASSEMBLY}

fun compile( files:List<Lexer>, stopAt: StopAt) : String {

    Log.setTestMode()
    allCodeBlocks.clear()

    val top = AstTop(nullLocation)
    for (file in files)
        Parser(file).parseTop(top)

    if (Log.allErrors.isNotEmpty())
        return Log.allErrors.joinToString(separator = "\n")
    if (stopAt == StopAt.AST)
        return top.dump()

    top.identifyClasses()
    top.identifyFunctions()
    top.generateIR()

    if (Log.allErrors.isNotEmpty())
        return Log.allErrors.joinToString(separator = "\n")
    if (stopAt == StopAt.IR)
        return top.dumpIR()

    allCodeBlocks.forEach { it.legalize() }
    allCodeBlocks.forEach { Peephole(it).run() }
    if (stopAt == StopAt.PEEPHOLE)
        return top.dumpIR()

    for (cb in allCodeBlocks) {
        val livemap = Livemap(cb)
        RegisterAllocator(cb, livemap).run()
        Peephole(cb).run()
    }
    if (Log.allErrors.isNotEmpty())
        return Log.allErrors.joinToString(separator = "\n")
    if (stopAt == StopAt.REGALLOC)
        return top.dumpIR()

    // Generate the final assembly
    calculateMemberOffsets()
    val sb = StringBuilder()
    //genAssemblyHeader(sb)
    for (cb in allCodeBlocks)
        cb.genAssembly(sb)
    //genAssemblyGlobalVars(sb)

    if (Log.allErrors.isNotEmpty())
        return Log.allErrors.joinToString(separator = "\n")
    return sb.toString()
}


fun String.runCommand(
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS
): String? = runCatching {
    ProcessBuilder("\\s".toRegex().split(this))
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start().also { it.waitFor(timeoutAmount, timeoutUnit) }
        .inputStream.bufferedReader().readText()
}.onFailure { it.printStackTrace() }.getOrNull()


