import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class PeepholeTest {
    private fun runTest(prog: String, expected: String) {
        val files = listOf(Lexer("input", StringReader(prog)))
        val result = compile(files, StopAt.PEEPHOLE)
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
            *****************************************************
                          TopLevel
            *****************************************************
            START
            END

            *****************************************************
                          sum
            *****************************************************
            START
            MOV array, %1
            MOV sum, 0
            MOV index, 0
            @1:
            MOV &5, 10
            BGTE_I index, &5, @3
            LSL_I &0, index, 2
            ADD_I &1, array, &0
            LDW &2, &1[0]
            ADD_I &3, sum, &2
            MOV sum, &3
            ADD_I &4, index, 1
            MOV index, &4
            JMP @1
            @3:
            MOV %8, sum
            END


        """.trimIndent()
        runTest(prog,expected)
    }

}