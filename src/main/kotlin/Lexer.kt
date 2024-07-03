import java.io.Reader
import TokenKind.*

class Lexer(private val fileName:String, private val fileHandle: Reader) {
    private var lineNumber = 1
    private var columnNumber = 1
    private var firstLine = 1
    private var firstColumn = 1
    private var lastLine = 1
    private var lastColumn = 1
    private var lineContinues = true
    private var atStartOfLine = true
    private var indentStack = mutableListOf(1)
    private var lookahead = readChar()
    private var atEof = false

    private fun readChar() : Char {
        val c = fileHandle.read()
        if (c!=-1)
            return c.toChar()
        atEof=true
        return '\n'
    }

    private fun nextChar() : Char {
        val ret = lookahead
        lastLine = lineNumber
        lastColumn = columnNumber
        lookahead = readChar()

        if (ret=='\n') {
            lineNumber++
            columnNumber=1
        } else
            columnNumber++
        return ret
    }

    private fun nextEscapedChar() : Char {
        val ret = nextChar()
        if (ret!='\\')
            return ret

        return when(val c = nextChar()) {
            'n' ->  '\n'
            't' ->  '\t'
            'r' ->  '\r'
            else -> c
        }
    }

    private fun skipWhitespaceAndComments() {
        while (lookahead==' ' || lookahead=='\t' || lookahead=='#' || (lookahead=='\n' && lineContinues && !atEof))
            if (lookahead=='#')
                while(!atEof && lookahead!='\n')
                    nextChar()
            else
                nextChar()
    }

    private fun readWord(): String {
        val sb = StringBuilder()
        do {
            sb.append(nextChar())
        } while(lookahead.isJavaIdentifierPart())
        return sb.toString()
    }

    private fun readString() : String {
        val sb = StringBuilder()
        nextChar()  // Get the opening "
        while(lookahead!='"' && !atEof)
            sb.append(nextEscapedChar())
        if (lookahead=='"')
            nextChar()
        else
            Log.error(makeLocation(), "Unterminated string")
        return sb.toString()
    }

    private fun readCharLit() : String {
        val sb = StringBuilder()
        nextChar()  // Get the opening '
        if (lookahead=='\'')
            Log.error(makeLocation(), "Empty char literal")
        sb.append(nextEscapedChar())
        if (lookahead=='\'')
            nextChar()
        else
            Log.error(makeLocation(), "Unterminated char literal")

        return sb.toString()
    }


    private fun readPunctuation() : String {
        val c = nextChar()
        return if ( (c=='!' && lookahead=='=') ||
            (c=='<' && lookahead=='=') ||
            (c=='>' && lookahead=='=') ||
            (c=='<' && lookahead=='<') ||
            (c=='>' && lookahead=='>') ||
            (c=='-' && lookahead=='>') ||
            (c=='?' && lookahead=='.') ||
            (c=='?' && lookahead==':') ||
            (c=='.' && lookahead=='.'))
            c.toString() + nextChar()
        else
            c.toString()
    }

    private fun makeLocation() = Location(fileName, firstLine, firstColumn, lastLine, lastColumn)

    fun nextToken() : Token {
        skipWhitespaceAndComments()
        firstLine = lineNumber
        firstColumn = columnNumber

        val kind : TokenKind
        val text : String

        if (atEof) {
            if (!atStartOfLine) {
                kind = EOL
            } else if (indentStack.size>1) {
                kind = DEDENT
                indentStack.removeLast()
            } else
                kind = EOF
            text = kind.text

        } else if (atStartOfLine && columnNumber>indentStack.last()) {
            kind = INDENT
            text = kind.text
            indentStack += columnNumber

        } else if (atStartOfLine && columnNumber<indentStack.last()) {
            kind = DEDENT
            text = kind.text
            indentStack.removeLast()
            if (columnNumber > indentStack.last()) {
                Log.error(makeLocation(), "Indentation error - expected column ${indentStack.last()}")
                indentStack += columnNumber
            }

        } else if (lookahead=='\n') {
            nextChar()
            kind = EOL
            text = kind.text

        } else if (lookahead.isJavaIdentifierStart()) {
            text = readWord()
            kind = TokenKind.kinds.getOrDefault(text,ID)

        } else if (lookahead.isDigit()) {
            text = readWord()
            kind = INTLIT

        } else if (lookahead=='"') {
            text = readString()
            kind = STRINGLIT

        } else if (lookahead=='\'') {
            text = readCharLit()
            kind = CHARLIT

        } else {
            text = readPunctuation()
            kind = TokenKind.kinds.getOrDefault(text,ERROR)
        }

        lineContinues = kind.lineContinues
        atStartOfLine = kind==EOL || kind==DEDENT

        return Token(makeLocation(), kind, text)
    }
}