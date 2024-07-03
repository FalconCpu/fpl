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

            *****************************************************
                          sum
            *****************************************************
            START
            MOV array, %1
            MOV sum, 0
            MOV index, 0
            JMP @1
            @3:
            LSL_I &1, index, 2
            ADD_I &2, array, &1
            LD4 &0, &2[0]
            ADD_I &3, sum, &0
            MOV sum, &3
            ADD_I &4, index, 1
            MOV index, &4
            @1:
            BLT_I index, 10, @3
            MOV %8, sum
            END


        """.trimIndent()
        runTest(prog,expected)
    }

}