import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class TypeCheck {
    private fun runTest(prog: String, expected: String) {
        val files = listOf(Lexer("input", StringReader(prog)))
        val result = compile(files, StopAt.IR)
        assertEquals(expected, result)
    }

    @Test
    fun simpleDeclarations() {
        val prog = """
            fun main()
                val a = 123
                val b = a + 1
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************

            *****************************************************
                          main
            *****************************************************
            START
            MOV a, 123
            ADD_I &0, a, 1
            MOV b, &0
            @0:
            END
            
            
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun typeErrors() {
        val prog = """
            fun main()
                val a = "hello"
                val b = a + 1
        """.trimIndent()

        val expected = """
            input:3.15-3.15: No operation defined for String + Int
        """.trimIndent()
        runTest(prog, expected)
    }

    @Test
    fun whileLoop() {
        val prog = """
            fun main()
                var a = 0
                while a<10
                    a=a+1
                end while
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************

            *****************************************************
                          main
            *****************************************************
            START
            MOV a, 0
            JMP @1
            @3:
            ADD_I &0, a, 1
            MOV a, &0
            @1:
            BLT_I a, 10, @3
            JMP @2
            @2:
            @0:
            END

            
        """.trimIndent()
        runTest(prog, expected)
    }

    @Test
    fun whileLoopNotBoolean() {
        val prog = """
            fun main()
                var a = 0
                while a
                    a=a+1
                end while
        """.trimIndent()

        val expected = """
            input:3.11-3.11: Condition must be of type bool not 'Int'
        """.trimIndent()
        runTest(prog, expected)
    }

    @Test
    fun ifTest() {
        val prog = """
            fun fred()
                var b=0
                val a = 5
                if a=1
                    b=2
                elsif a=2
                    b=3
                elsif a=3
                    b=4
                else
                    b=5
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************

            *****************************************************
                          fred
            *****************************************************
            START
            MOV b, 0
            MOV a, 5
            BEQ_I a, 1, @3
            JMP @2
            @2:
            BEQ_I a, 2, @5
            JMP @4
            @4:
            BEQ_I a, 3, @7
            JMP @6
            @6:
            MOV b, 5
            JMP @1
            @3:
            MOV b, 2
            JMP @1
            @5:
            MOV b, 3
            JMP @1
            @7:
            MOV b, 4
            JMP @1
            @1:
            @0:
            END


        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun ifNoElseTest() {
        val prog = """
            fun fred()
                var b=0
                val a = 5
                if a=1
                    b=2
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************

            *****************************************************
                          fred
            *****************************************************
            START
            MOV b, 0
            MOV a, 5
            BEQ_I a, 1, @3
            JMP @2
            @2:
            JMP @1
            @3:
            MOV b, 2
            JMP @1
            @1:
            @0:
            END


        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun typeAsValue() {
        val prog = """
            fun fred()
                var b=Int
        """.trimIndent()

        val expected = """
            input:2.11-2.13: Cannot use type name as expression
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun valueAsType() {
        val prog = """
            fun fred()
                var b=3
                val a:b = 7
        """.trimIndent()

        val expected = """
            input:3.11-3.11: Not a type
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun functionTypes() {
        val prog = """
            fun fred()
                val a: (Int,String)->Unit = 7
                val b: (Int,a)->Int
        """.trimIndent()

        val expected = """
            input:2.9-2.9: Cannot assign value of type Int to variable of type (Int, String)->Unit
            input:3.17-3.17: Not a type
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun functionCall() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
            
            fun main()
                val x = double(6)
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************

            *****************************************************
                          double
            *****************************************************
            START
            MOV a, %1
            MUL_I &0, a, 2
            MOV %8, &0
            JMP @0
            @0:
            END

            *****************************************************
                          main
            *****************************************************
            START
            MOV %1, 6
            CALL double
            MOV &0, %8
            MOV x, &0
            @0:
            END


        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun functionAsLvalue() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
            
            fun main()
                double = 4
        """.trimIndent()

        val expected = """
            input:5.5-5.10: Cannot assign value of type Int to variable of type (Int)->Int
            input:5.5-5.10: Not an lvalue
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun funcCallBadArgs() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
            
            fun main()
                val a = double(6,7)
                val b = double("hi")
                val c = double()               
        """.trimIndent()

        val expected = """
            input:5.19-5.19: Function expects 1 arguments, got 2
            input:6.20-6.23: Argument 1: Got type String when expecting Int
            input:7.19-7.19: Function expects 1 arguments, got 0
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun callAsStatement() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
            
            fun main()
                double(5)               
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************

            *****************************************************
                          double
            *****************************************************
            START
            MOV a, %1
            MUL_I &0, a, 2
            MOV %8, &0
            JMP @0
            @0:
            END

            *****************************************************
                          main
            *****************************************************
            START
            MOV %1, 5
            CALL double
            MOV &0, %8
            @0:
            END


        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun funcCallBadReturn() {
        val prog = """
            fun double(a:Int)->String
                return a*2
            
            fun triple(a:Int)->Int
                return
        """.trimIndent()

        val expected = """
            input:2.5-2.10: Function should return 'String', not 'Int'
            input:5.5-5.10: Function should return 'Int'
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun charLit() {
        val prog = """
            fun print(c:Char)
                return
                
            fun main()
                print('a')
                print('\n')
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************

            *****************************************************
                          print
            *****************************************************
            START
            MOV c, %1
            JMP @0
            @0:
            END

            *****************************************************
                          main
            *****************************************************
            START
            MOV %1, 97
            CALL print
            MOV %1, 10
            CALL print
            @0:
            END


        """.trimIndent()
        runTest(prog,expected)
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
            MUL_I &1, index, 4
            ADD_I &2, array, &1
            LD4 &0, &2[0]
            ADD_I &3, sum, &0
            MOV sum, &3
            ADD_I &4, index, 1
            MOV index, &4
            @1:
            BLT_I index, 10, @3
            JMP @2
            @2:
            MOV %8, sum
            JMP @0
            @0:
            END


        """.trimIndent()
        runTest(prog,expected)
    }



    @Test
    fun funcPointer() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
                
            fun main()
                val f = double
                val a = f(5)
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************

            *****************************************************
                          double
            *****************************************************
            START
            MOV a, %1
            MUL_I &0, a, 2
            MOV %8, &0
            JMP @0
            @0:
            END

            *****************************************************
                          main
            *****************************************************
            START
            MOV f, double
            MOV %1, 5
            CALLR f
            MOV &0, %8
            MOV a, &0
            @0:
            END


        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun funcPointerWrongArgs() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
                
            fun main()
                val f = double
                val a = f("5")
        """.trimIndent()

        val expected = """
            input:6.15-6.17: Argument 1: Got type String when expecting Int
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun callNonFunc() {
        val prog = """
            fun main()
                val f = 5+6
                val a = f("5")
        """.trimIndent()

        val expected = """
            input:3.14-3.14: Cannot call non-function
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun classTypes() {
        val prog = """
            class Animal(val name:String, var legs:Int)

            fun fred(a:Animal)->Int
                a.legs = a.legs - 1
                return a.legs
                
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************

            *****************************************************
                          Animal
            *****************************************************
            START
            ST4 %2, %1[name]
            ST4 %3, %1[legs]
            END

            *****************************************************
                          fred
            *****************************************************
            START
            MOV a, %1
            LD4 &0, a[legs]
            SUB_I &1, &0, 1
            ST4 &1, a[legs]
            LD4 &2, a[legs]
            MOV %8, &2
            JMP @0
            @0:
            END


        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun writeToImmutableField() {
        val prog = """
            class Animal(val name:String, val legs:Int)

            fun fred(a:Animal)->Int
                a.legs = a.legs - 1
                return a.legs
                
        """.trimIndent()

        val expected = """
            input:4.7-4.10: Cannot assign to immutable field legs
        """.trimIndent()
        runTest(prog,expected)
    }

}