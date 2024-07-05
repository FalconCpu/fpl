import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class ParserTest {
    private fun runTest(prog:String, expected:String) {
        val files = listOf( Lexer("input",StringReader(prog)))
        val result = compile(files, StopAt.AST)
        assertEquals(expected,result)
    }

    @Test
    fun simpleDeclarations() {
        val prog = """
            val a = 123
            var b = a/2 + 5*3
        """.trimIndent()

        val expected = """
            TOP
              val a
                INTLIT 123
              var b
                +
                  /
                    ID a
                    INTLIT 2
                  *
                    INTLIT 5
                    INTLIT 3

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun functionDeclarations() {
        val prog = """
            fun fred(a:Int,b:String)->Int
                val x = 4
        """.trimIndent()

        val expected = """
            TOP
              FUNC fred
                PARAM a
                  ID Int
                PARAM b
                  ID String
                ID Int
                val x
                  INTLIT 4

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun functionMissingBody() {
        val prog = """
            fun fred(a:Int,b:String)->Int

            val x = 4
        """.trimIndent()

        val expected = """
            input:3.1-3.3: Expected indented block
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun functionMissingParams() {
        val prog = """
            fun fred->Int

            val x = 4
        """.trimIndent()

        val expected = """
            input:1.9-1.10: Got '->' when expecting '('
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun functionBadEnd() {
        val prog = """
            fun fred(a:Int)->Int
                val x = 4
            end while
        """.trimIndent()

        val expected = """
            input:3.5-3.9: Got 'while' when expecting 'fun'
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun functionNoParams() {
        val prog = """
            fun fred()->Int
                val x = 4
        """.trimIndent()

        val expected = """
            TOP
              FUNC fred
                ID Int
                val x
                  INTLIT 4

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun functionBadBody() {
        val prog = """
            fun fred(a:Int)->Int
                val x = 4
                -c
        """.trimIndent()

        val expected = """
            input:3.5-3.5: Got '-' when expecting statement
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun whileTest() {
        val prog = """
            fun fred(a:Int)->Int
                while(a<10)
                    a=a+1
                return a
        """.trimIndent()

        val expected = """
            TOP
              FUNC fred
                PARAM a
                  ID Int
                ID Int
                WHILE
                  <
                    ID a
                    INTLIT 10
                  ASSIGN
                    ID a
                    +
                      ID a
                      INTLIT 1
                RETURN
                  ID a

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun declWithNoInitValue() {
        val prog = """
            fun fred(a:Int)->Int
                val x : Int
                x = 1
        """.trimIndent()

        val expected = """
            TOP
              FUNC fred
                PARAM a
                  ID Int
                ID Int
                val x
                  ID Int
                ASSIGN
                  ID x
                  INTLIT 1

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun badPrimaryExpression() {
        val prog = """
            fun fred(a:Int)->Int
                val x = 3 + )
        """.trimIndent()

        val expected = """
            input:2.17-2.17: Got ')' when expecting primary expression
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun badTopStatement() {
        val prog = """
            while a<1
                : 3
        """.trimIndent()

        val expected = """
            input:1.1-1.5: Invalid statement in top level
            input:2.5-2.5: Got ':' when expecting statement
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun funcCalls() {
        val prog = """
            fun fred()
                val a = foo(1,2,3)
                val s = bar()
                baz(s)
        """.trimIndent()

        val expected = """
            TOP
              FUNC fred
                val a
                  FuncCall
                    ID foo
                    INTLIT 1
                    INTLIT 2
                    INTLIT 3
                val s
                  FuncCall
                    ID bar
                FuncCall
                  ID baz
                  ID s

        """.trimIndent()
        runTest(prog,expected)
    }


    @Test
    fun index() {
        val prog = """
            fun fred()
                val a = foo[4]
        """.trimIndent()

        val expected = """
            TOP
              FUNC fred
                val a
                  index
                    ID foo
                    INTLIT 4

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun andOr() {
        val prog = """
            fun fred()
                while a<2 and b>4 or c=1
                    val a = foo[4]
        """.trimIndent()

        val expected = """
            TOP
              FUNC fred
                WHILE
                  OR
                    AND
                      <
                        ID a
                        INTLIT 2
                      >
                        ID b
                        INTLIT 4
                    =
                      ID c
                      INTLIT 1
                  val a
                    index
                      ID foo
                      INTLIT 4

        """.trimIndent()
        runTest(prog,expected)
    }


    @Test
    fun ifTest() {
        val prog = """
            fun fred()
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
            TOP
              FUNC fred
                IF
                  CLAUSE
                    =
                      ID a
                      INTLIT 1
                    ASSIGN
                      ID b
                      INTLIT 2
                  CLAUSE
                    =
                      ID a
                      INTLIT 2
                    ASSIGN
                      ID b
                      INTLIT 3
                  CLAUSE
                    =
                      ID a
                      INTLIT 3
                    ASSIGN
                      ID b
                      INTLIT 4
                  CLAUSE
                    ASSIGN
                      ID b
                      INTLIT 5

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun invalidNestFunc() {
        val prog = """
            fun fred()
                val a = 1
                fun bar()
                    val b = 2
        """.trimIndent()

        val expected = """
            input:3.5-3.7: Functions are not allowed to nest
        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun arrayType() {
        val prog = """
            fun fred(a:Array<Int>)
                val x = a[3]
        """.trimIndent()

        val expected = """
            TOP
              FUNC fred
                PARAM a
                  []
                    ID Int
                val x
                  index
                    ID a
                    INTLIT 3

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun classTypes() {
        val prog = """
            class Animal(val name:String, val legs:Int)

            fun fred(a:Animal)->Int
                return a.legs
                
        """.trimIndent()

        val expected = """
            TOP
              CLASS Animal
                PARAM name
                  ID String
                PARAM legs
                  ID Int
              FUNC fred
                PARAM a
                  ID Animal
                ID Int
                RETURN
                  MEMBER legs
                    ID a

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun constTest() {
        val prog = """
            const one = 1
                
            fun main()->Int
                return 3 + one
                
        """.trimIndent()

        val expected = """
            TOP
              CONST one
                INTLIT 1
              FUNC main
                ID Int
                RETURN
                  +
                    INTLIT 3
                    ID one

        """.trimIndent()
        runTest(prog,expected)
    }

    @Test
    fun forLoop() {
        val prog = """
            fun main()->Int
                var total = 0
                for i in 1..10
                    total = total + i
                return total
        """.trimIndent()

        val expected = """
            TOP
              FUNC main
                ID Int
                var total
                  INTLIT 0
                FOR i
                  INTLIT 1
                  INTLIT 10
                  ASSIGN
                    ID total
                    +
                      ID total
                      ID i
                RETURN
                  ID total

        """.trimIndent()
        runTest(prog,expected)
    }


    @Test
    fun repeatLoop() {
        val prog = """
            fun main()->Int
                var total = 0
                repeat
                    total = total + 1
                until total > 10
                return total

        """.trimIndent()

        val expected = """
            TOP
              FUNC main
                ID Int
                var total
                  INTLIT 0
                REPEAT
                  >
                    ID total
                    INTLIT 10
                  ASSIGN
                    ID total
                    +
                      ID total
                      INTLIT 1
                RETURN
                  ID total

        """.trimIndent()
        runTest(prog,expected)
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
            TOP
              FUNC foo
                ID Int
                val a
                  CONSTRUCTOR true
                    []
                      ID Int
                    INTLIT 10
                ASSIGN
                  index
                    ID a
                    INTLIT 3
                  INTLIT 4
                RETURN
                  index
                    ID a
                    INTLIT 3

        """.trimIndent()
        runTest(prog,expected)
    }




}