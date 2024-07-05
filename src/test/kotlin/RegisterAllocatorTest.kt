import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class RegisterAllocatorTest {
    private fun runTest(prog: String, expected: String) {
        val files = listOf(Lexer("input", StringReader(prog)))
        val result = compile(files, StopAt.REGALLOC)
        assertEquals(expected, result)
    }

    @Test
    fun sumArray() {
        val prog = """
            fun sum(array:Array<Int>)->Int
                var sum = 0
                var index = 0
                while index < 10
                    sum = sum + array[index]
                    index = index + 1                
                return sum
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************
            START
            END

            *****************************************************
                          sum
            *****************************************************
            START
            MOV %8, 0
            MOV %2, 0
            @1:
            MOV %3, 10
            BGTE_I %2, %3, @3
            LSL_I %3, %2, 2
            ADD_I %3, %1, %3
            LDW %3, %3[0]
            ADD_I %8, %8, %3
            ADD_I %2, %2, 1
            JMP @1
            @3:
            END


        """.trimIndent()
        runTest(prog,expected)
    }

}