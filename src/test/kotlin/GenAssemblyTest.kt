import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class GenAssemblyTest {
    private fun runTest(prog: String, expected: String) {
        val files = listOf(Lexer("input", StringReader(prog)))
        val result = compile(files, StopAt.ASSEMBLY)
        assertEquals(expected, result)
    }

    @Test
    fun sumArray() {
        val prog = """
            fun sum(array:Int[])->Int
                var sum = 0
                var index = 0
                while index < 10
                    sum = sum + array[index]
                    index = index + 1                
                return sum
        """.trimIndent()

        val expected = """
            TopLevel:
            ret
            sum:
            ld %8, 0
            ld %2, 0
            jmp .@1
            .@3:
            lsl %3, %2, 2
            add %3, %1, %3
            ldw %3, %3[0]
            add %8, %8, %3
            add %2, %2, 1
            .@1:
            blt %2, 10, .@3
            ret

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun factorial() {
        val prog = """
            fun factorial(n:Int)->Int
                if n = 0
                    return 1
                else
                    return n * factorial(n - 1)
        """.trimIndent()

        val expected = """
            TopLevel:
            ret
            factorial:
            sub %sp, %sp, 8
            stw %9, %sp[0]
            stw %30, %sp[4]
            ld %9, %1
            beq %9, 0, .@3
            sub %1, %9, 1
            jsr factorial
            mul %8, %9, %8
            jmp .@0
            .@3:
            ld %8, 1
            .@0:
            ldw %9, %sp[0]
            ldw %30, %sp[4]
            add %sp, %sp, 8
            ret

        """.trimIndent()
        runTest(prog,expected)
    }


}