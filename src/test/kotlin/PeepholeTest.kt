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

    @Test
    fun nullableAnd() {
        val prog = """
            class LinkedList(val next:LinkedList?, val item:Int)

            fun foo(var list:LinkedList?)->Int
                if list!= null and list.item > 0     # this should not give an error as guarded by a null check
                    return 1
                else
                    return 0
            """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************
            START
            END

            *****************************************************
                          LinkedList
            *****************************************************
            START
            MOV this, %1
            MOV &0, %2
            STW &0, this[next]
            MOV &1, %3
            STW &1, this[item]
            END

            *****************************************************
                          foo
            *****************************************************
            START
            MOV list, %1
            BEQ_I list, 0, @5
            LDW &0, list[item]
            BLTE_I &0, 0, @5
            MOV %8, 1
            JMP @0
            @5:
            MOV %8, 0
            @0:
            END


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun localArray() {
        val prog = """
            fun foo()->Int
                val a = local Array<Int>(10)
                a[3] = 4
                return a[3]
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************
            START
            END

            *****************************************************
                          foo
            *****************************************************
            START
            SUB_I &0, %sp, 40
            MOV %sp, &0
            MOV a, &0
            MOV &3, 4
            STW &3, a[12]
            MOV %8, 4
            END


        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun globalVarTest() {
        val prog = """
            var a = 0
            var b : Int       # should get default value of 0
            fun main()->Int
                a=a+1
                return a+b
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************
            START
            STW 0, %29[a]
            STW 0, %29[b]
            END

            *****************************************************
                          main
            *****************************************************
            START
            LDW &0, %29[a]
            ADD_I &1, &0, 1
            MOV &0, &1
            STW &0, %29[a]
            LDW &2, %29[b]
            ADD_I &3, &0, &2
            MOV %8, &3
            END

            
        """.trimIndent()
        runTest(prog, expected)
    }



}