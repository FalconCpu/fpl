import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class PathCheck {
    private fun runTest(prog: String, expected: String) {
        val files = listOf(Lexer("input", StringReader(prog)))
        val result = compile(files, StopAt.IR)
        assertEquals(expected, result)
    }


    @Test
    fun nullableType() {
        val prog = """
            class Animal(val name:String, var legs:Int)

            fun fred(a:Animal?)->Int
                return a.legs       # this should give an error as a could be null
                
        """.trimIndent()

        val expected = """
            input:4.14-4.17: Cannot access member as reference could be null
        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun smartNullableType() {
        val prog = """
            class Animal(val name:String, var legs:Int)

            fun fred(a:Animal?)->Int
                if (a!= null)
                    return a.legs   # this is fine as the access is guarded by a null check
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
                          Animal
            *****************************************************
            START
            MOV this, %1
            MOV &0, %2
            STW &0, this[name]
            MOV &1, %3
            STW &1, this[legs]
            END

            *****************************************************
                          fred
            *****************************************************
            START
            MOV a, %1
            BNE_I a, 0, @3
            JMP @2
            @2:
            JMP @4
            @3:
            LDW &0, a[legs]
            MOV %8, &0
            JMP @0
            JMP @1
            @4:
            MOV %8, 0
            JMP @0
            JMP @1
            @1:
            @0:
            END


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun nullableType2() {
        val prog = """
            class Animal(val name:String, var legs:Int)

            fun fred(a:Animal?)->Int
                if a!=null
                    val b = 1
                return a.legs       # this should give an error as a could be null, despite checking for null in if
                
        """.trimIndent()

        val expected = """
            input:6.14-6.17: Cannot access member as reference could be null
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun nullableType3() {
        val prog = """
            class Animal(val name:String, var legs:Int)

            fun fred(a:Animal?)->Int
                if a=null
                    return 0
                return a.legs       # this should be OK - as flow doesn't reach the return if a is null
                
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************
            START
            END

            *****************************************************
                          Animal
            *****************************************************
            START
            MOV this, %1
            MOV &0, %2
            STW &0, this[name]
            MOV &1, %3
            STW &1, this[legs]
            END

            *****************************************************
                          fred
            *****************************************************
            START
            MOV a, %1
            BEQ_I a, 0, @3
            JMP @2
            @2:
            JMP @1
            @3:
            MOV %8, 0
            JMP @0
            JMP @1
            @1:
            LDW &0, a[legs]
            MOV %8, &0
            JMP @0
            @0:
            END


        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun nullableWhile() {
        val prog = """
            class LinkedList(val next:LinkedList?, val item:Int)

            fun total(var list:LinkedList?)->Int
                var total = 0
                while list!=null
                    total = total + list.item  # this should not give an error as guarded by a null check
                return total
                
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
                          total
            *****************************************************
            START
            MOV list, %1
            MOV total, 0
            @1:
            BNE_I list, 0, @2
            JMP @3
            @2:
            LDW &0, list[item]
            ADD_I &1, total, &0
            MOV total, &1
            JMP @1
            @3:
            MOV %8, total
            JMP @0
            @0:
            END


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun uninitializedVar() {
        val prog = """
            fun main()->Int
                val a : Int
                var b = a + 1     # this should give an error as a is uninitialized
                return b
        """.trimIndent()

        val expected = """
            input:3.13-3.13: Variable 'a' is uninitialized
        """.trimIndent()
        runTest(prog, expected)
    }

    @Test
    fun uninitializedVar2() {
        val prog = """
            fun main()->Int
                val a : Int
                a = 4            # Writing to immutable variable OK as it is unitialized
                var b = a + 1     
                return b
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************
            START
            END

            *****************************************************
                          main
            *****************************************************
            START
            MOV a, 4
            ADD_I &0, a, 1
            MOV b, &0
            MOV %8, b
            JMP @0
            @0:
            END

            
        """.trimIndent()
        runTest(prog, expected)
    }

    @Test
    fun uninitializedVar3() {
        val prog = """
            fun main(x:Int)->Int
                val a : Int
                if x > 0
                    a = 1
                a = 4            # Not OK as a might have been initialized
        """.trimIndent()

        val expected = """
            input:5.5-5.5: Immutable variable 'a' may already be initialized
        """.trimIndent()
        runTest(prog, expected)
    }

    @Test
    fun uninitializedVar4() {
        val prog = """
            fun main(x:Int)->Int
                val a : Int
                if x > 0
                    a = 1
                return a + 1     # Not OK as a might not be initialized
        """.trimIndent()

        val expected = """
            input:5.12-5.12: Variable 'a' may be uninitialized
        """.trimIndent()
        runTest(prog, expected)
    }


    @Test
    fun uninitializedVar5() {
        val prog = """
            fun main(x:Int)->Int
                val a : Int
                if x > 0
                    a = 1
                else
                    a = 2
                return a + 1     # OK as a is initialized on either branch
        """.trimIndent()

        val expected = """
            *****************************************************
                          TopLevel
            *****************************************************
            START
            END

            *****************************************************
                          main
            *****************************************************
            START
            MOV x, %1
            BGT_I x, 0, @3
            JMP @2
            @2:
            JMP @4
            @3:
            MOV a, 1
            JMP @1
            @4:
            MOV a, 2
            JMP @1
            @1:
            ADD_I &0, a, 1
            MOV %8, &0
            JMP @0
            @0:
            END

            
        """.trimIndent()
        runTest(prog, expected)
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
            BNE_I list, 0, @4
            JMP @2
            @4:
            LDW &0, list[item]
            BGT_I &0, 0, @3
            JMP @2
            @2:
            JMP @5
            @3:
            MOV %8, 1
            JMP @0
            JMP @1
            @5:
            MOV %8, 0
            JMP @0
            JMP @1
            @1:
            @0:
            END


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun nullableOr() {
        val prog = """
            class LinkedList(val next:LinkedList?, val item:Int)

            fun foo(var list:LinkedList?)->Int
                if list=null or list.item < 0     # this should not give an error as guarded by a null check
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
            BEQ_I list, 0, @3
            JMP @4
            @4:
            LDW &0, list[item]
            BLT_I &0, 0, @3
            JMP @2
            @2:
            JMP @5
            @3:
            MOV %8, 1
            JMP @0
            JMP @1
            @5:
            MOV %8, 0
            JMP @0
            JMP @1
            @1:
            @0:
            END


        """.trimIndent()

        runTest(prog, expected)
    }


}