
class Token (
    val location : Location,
    val kind : TokenKind,
    val text : String
) {
    override fun toString() = text
}

enum class TokenKind (val text:String, val lineContinues:Boolean) {
    EOF            ("<end of file>", false),
    EOL            ("<end of line>", true),
    INDENT         ("<indent>", false),
    DEDENT         ("<dedent>", false),
    ID             ("<identifier>", false),
    INTLIT         ("<int literal>", false),
    CHARLIT        ("<char literal>", false),
    REALLIT        ("<real literal>", false),
    STRINGLIT      ("<string literal>", false),
    PLUS           ("+", true),
    MINUS          ("-", true),
    STAR           ("*", false),
    SLASH          ("/", true),
    PERCENT        ("%", true),
    AMPERSAND      ("&", true),
    BAR            ("|", true),
    CARAT          ("^", true),
    EQ             ("=", true),
    NEQ            ("!=", true),
    LT             ("<", true),
    LTE            ("<=", true),
    GT             (">", true),
    GTE            (">=", true),
    LEFT           ("<<", true),
    RIGHT          (">>", true),
    AND            ("and", true),
    OR             ("or", true),
    NOT            ("not", true),
    NEW            ("new", true),
    DOT            (".", true),
    COMMA          (",", true),
    COLON          (":", true),
    IN             ("in", true),
    DOTDOT         ("..", true),
    ARROW          ("->", true),
    QMARK          ("?", false),
    QMARKDOT       ("?.", true),
    QMARKCOLON     ("?:", true),
    EMARK          ("!", true),
    OPENB          ("(", true),
    OPENSQ         ("[", true),
    OPENCL         ("{", true),
    CLOSEB         (")", false),
    CLOSESQ        ("]", false),
    CLOSECL        ("}", false),
    VAL            ("val", false),
    VAR            ("var", false),
    IF             ("if", false),
    ELSIF          ("elsif", false),
    ELSE           ("else", false),
    THEN           ("then", true),
    END            ("end", false),
    FOR            ("for", false),
    REPEAT         ("repeat", false),
    UNTIL          ("until", false),
    WHILE          ("while", false),
    CONST          ("const", false),
    RETURN         ("return", false),
    CLASS          ("class", false),
    ENUM           ("enum", false),
    FUN            ("fun", false),
    ARRAY          ("Array", false),
    LOCAL          ("local", false),
    ERROR          ("<error>", false);

    override fun toString() = text

    companion object {
        val kinds = entries.associateBy { it.text }
    }
}