import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.io.StringReader

private val stdLibFiles = listOf(
    "src/main/stdlib/MemMappedRegs.fpl"
)


class StdLibTest {
    fun runTest(prog: String, expected: String) {
        val lexers = listOf(Lexer("input", StringReader(prog))) + stdLibFiles.map { Lexer(it, File(it).reader()) }


        val asm = compile(lexers, StopAt.ASSEMBLY)
        if (Log.allErrors.isNotEmpty()) {
            println(Log.allErrors.joinToString(separator = "\n"))
            error("Compilation failed")
        }

        File("a.f32").writeText("jmp main\n$asm")

        val asmOut = "f32asm a.f32".runCommand()
        assertEquals("", asmOut)

        val a4 = "f32sim -d rom.hex".runCommand()
        assertEquals(expected, a4?.replace("\r\n", "\n"))
    }

    @Test
    fun kPrintCharTest() {
        val prog = """
            fun main()
                kPrintChar('H')
                kPrintChar('e')
                kPrintChar('l')
                kPrintChar('l')
                kPrintChar('o')
                kPrintChar('\n')                
                
        """.trimIndent()

        val expected = """
            Hello
            
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun kPrintStringTest() {
        val prog = """
            fun main()
                kPrintString("Hello World!\n")
        """.trimIndent()

        val expected = """
            Hello World!
            
        """.trimIndent()

        runTest(prog, expected)
    }


}