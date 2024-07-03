import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader
import TokenKind.*

class LexerTest {
    @Test
    fun lexerTest() {
        val text = """
            if a<=12 
               b=3
            # some comment
            end 
        """.trimIndent()

        val expected = listOf(
            "if" to IF,
            "a" to ID,
            "<=" to LTE,
            "12" to INTLIT,
            "<end of line>" to EOL,
            "<indent>" to INDENT,
            "b" to ID,
            "=" to EQ,
            "3" to INTLIT,
            "<end of line>" to EOL,
            "<dedent>" to DEDENT,
            "end" to END,
            "<end of line>" to EOL,
            "<end of file>" to EOF
        )

        val lexer = Lexer("input", StringReader(text))
        for (e in expected) {
            val tok = lexer.nextToken()
            assertEquals(e.first, tok.text)
            assertEquals(e.second, tok.kind)
        }
    }

}