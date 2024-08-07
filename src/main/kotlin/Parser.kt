import TokenKind.*

class Parser(private val lexer: Lexer) {
    private var lookahead = lexer.nextToken()

    private fun nextToken() : Token {
        val ret = lookahead
        lookahead = lexer.nextToken()
        return ret
    }

    private fun expect(kind: TokenKind) : Token {
        if (lookahead.kind==kind)
            return nextToken()
        throw ParseError(lookahead.location, "Got '$lookahead' when expecting '${kind.text}'")
    }

    private fun canTake(kind: TokenKind) : Boolean {
        if (lookahead.kind==kind) {
            nextToken()
            return true
        }
        return false
    }

    private fun skipToEol() {
        while(lookahead.kind!=EOF && lookahead.kind!=EOL)
            nextToken()
        nextToken()
    }

    private fun expectEol() {
        if (lookahead.kind!=EOL)
            Log.error(lookahead.location, "Got '$lookahead' when expecting end of line")
        skipToEol()
    }

    private fun parseId(): Ast {
        val id = expect(ID)
        return AstId(id.location, id.text)
    }

    private fun parseIntLit(): AstIntLit {
        val id = expect(INTLIT)
        return AstIntLit(id.location, id.text)
    }

    private fun parseCharLit() : AstCharLit {
        val id = expect(CHARLIT)
        return AstCharLit(id.location, id.text)
    }

    private fun parseStringLit(): AstStringLit {
        val id = expect(STRINGLIT)
        return AstStringLit(id.location, id.text)
    }

    private fun parseBracket() : Ast {
        expect(OPENB)
        val ret = parseExpression()
        if (canTake(COLON)) {
            val type = parseType()
            val loc = expect(CLOSEB)
            return AstCast(loc.location, ret, type)
        }
        expect(CLOSEB)
        return ret
    }

    private fun parsePrimary() : Ast {
        return when(lookahead.kind) {
            ID -> parseId()
            INTLIT -> parseIntLit()
            STRINGLIT -> parseStringLit()
            CHARLIT -> parseCharLit()
            OPENB -> parseBracket()
            else -> throw ParseError(lookahead.location, "Got '$lookahead' when expecting primary expression")
        }
    }

    private fun parseFuncCall(lhs:Ast) : Ast {
        return AstFuncCall(lookahead.location, lhs, parseExpressionList())
    }

    private fun parseConstructor() : Ast {
        val loc = nextToken()
        val type = parseType()
        val args = parseExpressionList()
        return AstConstructor(Location(loc.location,type.location), type, args, loc.kind==LOCAL)
    }

    private fun parseIndex(lhs:Ast) : Ast {
        expect(OPENSQ)
        val rhs = parseExpression()
        expect(CLOSESQ)
        return AstIndex(lookahead.location, lhs, rhs)
    }

    private fun parseMember(lhs: Ast): Ast {
        expect(DOT)
        val id = expect(ID)
        return AstMember(id.location, lhs, id.text)
    }

    private fun parseNullishMember(lhs: Ast): Ast {
        expect(QMARKDOT)
        val id = expect(ID)
        return AstNullMember(id.location, lhs, id.text)
    }


    private fun parsePostfix() : Ast {
        var ret = parsePrimary()
        while(true)
            ret = when(lookahead.kind) {
                OPENB -> parseFuncCall(ret)
                OPENSQ -> parseIndex(ret)
                DOT -> parseMember(ret)
                QMARKDOT -> parseNullishMember(ret)
                else -> return ret
            }
    }

    private fun parsePrefix() : Ast {
        return when(lookahead.kind) {
            NEW, LOCAL -> {
                parseConstructor()
            }

            MINUS -> {
                expect(MINUS)
                AstUnaryMinus(lookahead.location, parsePrefix())
            }

            else -> parsePostfix()
        }
    }

    private fun parseMult() : Ast {
        var ret = parsePrefix()
        while(lookahead.kind in listOf(STAR,SLASH,PERCENT,AMPERSAND,LEFT,RIGHT)) {
            val op = nextToken()
            ret = AstBinop(op.location, op.kind, ret, parsePrefix())
        }
        return ret
    }

    private fun parseAdd() : Ast {
        var ret = parseMult()
        while(lookahead.kind in listOf(PLUS, MINUS, BAR, CARAT)) {
            val op = nextToken()
            ret = AstBinop(op.location, op.kind, ret, parseMult())
        }
        return ret
    }

    private fun parseRange() : Ast {
        val ret = parseAdd()
        if (canTake(DOTDOT)) {
            val inclusive = ! canTake(LT)
            val end = parseAdd()
            return AstRange(Location(ret.location, end.location), ret, end, inclusive)
        }
        return ret
    }

    private fun parseComp() : Ast {
        var ret = parseRange()
        while(lookahead.kind in listOf(EQ,NEQ,LT,LTE,GT,GTE)) {
            val op = nextToken()
            ret = AstComp(op.location, op.kind, ret, parseRange())
        }
        return ret
    }

    private fun parseAnd() : Ast {
        var ret = parseComp()
        while(lookahead.kind==AND) {
            val op = nextToken()
            ret = AstAnd(op.location, ret, parseComp())
        }
        return ret
    }

    private fun parseOr() : Ast {
        var ret = parseAnd()
        while(lookahead.kind==OR) {
            val op = nextToken()
            ret = AstOr(op.location, ret, parseAnd())
        }
        return ret
    }

    private fun parseExpression() : Ast {
        return parseOr()
    }

    private fun parseExpressionList() : List<Ast> {
        val ret = mutableListOf<Ast>()
        expect(OPENB)
        if (canTake(CLOSEB))
            return ret
        do {
            ret += parseExpression()
        } while (canTake(COMMA))
        expect(CLOSEB)
        return ret
    }

    private fun parseTypeList(): List<Ast> {
        val ret = mutableListOf<Ast>()
        expect(OPENB)
        if (canTake(CLOSEB))
            return ret
        do {
            ret += parseType()
        } while (canTake(COMMA))
        expect(CLOSEB)
        return ret
    }

    private fun parseTypeBracket(): Ast {
        val terms = parseTypeList()
        if (canTake(ARROW)) {
            val retType = parseType()
            return AstFuncType(lookahead.location, terms, retType)
        } else if (terms.size==1)
            return terms[0]
        else
            throw ParseError(lookahead.location, "Multiple types in type bracket")
    }

    private fun parseTypeArray(): Ast {
        val array = expect(ARRAY)
        expect(LT)
        val base = parseType()
        val gt = expect(GT)
        return AstArrayType(Location(array.location, gt.location), base)
    }

    private fun parseType() : Ast {
        val ret = when(lookahead.kind) {
            ID    -> parseId()
            ARRAY -> parseTypeArray()
            OPENB -> parseTypeBracket()
            else -> throw ParseError(lookahead.location, "Got '$lookahead' when expecting type")
        }

        if (lookahead.kind==QMARK) {
            val qm = nextToken()
            return AstQmark(Location(ret.location,qm.location), ret)
        }
        return ret
    }

    private fun parseOptExpr() : Ast? {
        if (canTake(EQ))
            return parseExpression()
        return null
    }

    private fun parseOptType() : Ast? {
        if (canTake(COLON))
            return parseType()
        return null
    }


    private fun parseDecl(block: AstBlock)  {
        val kind = nextToken()
        val id = expect(ID)
        val type = parseOptType()
        val expr = parseOptExpr()
        expectEol()
        block.add( AstDecl(id.location, kind.kind, id.text, type, expr))
    }

    private fun parseConst(block: AstBlock) {
        expect(CONST)
        val id = expect(ID)
        val type = parseOptType()
        expect(EQ)
        val expr = parseExpression()
        expectEol()
        block.add(AstConst(id.location, id.text, type, expr))
    }


    private fun parseAssign(block: AstBlock){
        val lhs = parsePostfix()

        if (canTake(EOL)) {
            if (lhs !is AstFuncCall)
                Log.error(lhs.location,"Expression has no effect")
            block.add(lhs)
        } else {
            expect(EQ)
            val rhs = parseExpression()
            expectEol()
            block.add(AstAssign(rhs.location, lhs, rhs))
        }
    }

    private fun parseReturn(block: AstBlock) {
        val loc  = expect(RETURN)
        val expr = if (lookahead.kind!=EOL) parseExpression() else null
        expectEol()
        block.add(AstReturn(loc.location, expr))
    }

    private fun parseParamList() : List<AstParam> {
        val ret = mutableListOf<AstParam>()
        expect(OPENB)
        if (canTake(CLOSEB))
            return ret
        do {
            val v = if (canTake(VAR)) VAR else if (canTake(VAL)) VAL else EOF
            val id = expect(ID)
            expect(COLON)
            val type = parseType()
            ret += AstParam(Location(id.location,type.location), v, id.text, type)
        } while(canTake(COMMA))
        expect(CLOSEB)
        return ret
    }

    private fun parseClass(block: AstBlock) {
        val loc = expect(CLASS)
        val id = expect(ID)
        val params = if (lookahead.kind==OPENB) parseParamList() else emptyList()
        val superclass = parseOptType()
        expectEol()
        val cls = AstClass(loc.location, block, id.text, params,  superclass)
        if (lookahead.kind==INDENT) {
            parseBlock(cls)
            checkEnd(CLASS)
        }
        block.add(cls)
    }

    private fun parseIdList(): List<AstId> {
        val ret = mutableListOf<AstId>()
        expect(INDENT)
        while (lookahead.kind != DEDENT && lookahead.kind != EOF) {
            val id = expect(ID)
            ret += AstId(id.location, id.text)
            expectEol()
        }
        expect(DEDENT)
        return ret
    }

    private fun parseEnum(block: AstBlock) {
        expect(ENUM)
        val id = expect(ID)
        expectEol()
        val members = parseIdList()
        checkEnd(ENUM)
        block.add(AstEnum(id.location, id.text, members))
    }

    private fun parseBlock(block:AstBlock) {
        if (lookahead.kind!=INDENT) {
            Log.error(lookahead.location,"Expected indented block")
            return
        }
        expect(INDENT)
        while(lookahead.kind!=DEDENT && lookahead.kind!=EOF)
            try {
                parseStatement(block)
            } catch (e:ParseError) {
                Log.error(e.message!!)
                skipToEol()
            }
        expect(DEDENT)
        return
    }

    private fun checkEnd(kind: TokenKind) {
        if (canTake(END)) {
            if (lookahead.kind!=EOL)
                expect(kind)
            expectEol()
        }
    }

    private fun parseFun(block:AstBlock) {
        val f = expect(FUN)
        val id = expect(ID)
        val params = parseParamList()
        val retType = if (canTake(ARROW)) parseType() else null
        expectEol()
        val func = AstFunction(f.location, block, id.text, params, retType)
        block.add(func)
        parseBlock(func)
        checkEnd(FUN)
    }

    private fun parseWhile(block: AstBlock) {
        val f = expect(WHILE)
        val cond = parseExpression()
        expectEol()
        val stmt = AstWhile(f.location, block, cond)
        block.add(stmt)
        parseBlock(stmt)
        checkEnd(WHILE)
    }

    private fun parseRepeat(block: AstBlock) {
        val f = expect(REPEAT)
        expectEol()
        val stmt = AstRepeat(f.location, block)
        block.add(stmt)
        parseBlock(stmt)
        expect(UNTIL)
        stmt.cond = parseExpression()
        expectEol()
    }

    private fun parseFor(block: AstBlock) {
        val f = expect(FOR)
        val id = expect(ID)
        expect(IN)
        val expr = parseExpression()
        expectEol()
        val stmt = AstFor(f.location, block, id.text, expr)
        block.add(stmt)
        parseBlock(stmt)
        checkEnd(FOR)
    }

    private fun parseClause(block: AstBlock) : AstClause{
        val cond = parseExpression()
        expectEol()
        val stmt = AstClause(cond.location, block, cond)
        parseBlock(stmt)
        return stmt
    }

    private fun parseElse(block: AstBlock) : AstClause{
        val loc = lookahead
        expectEol()
        val stmt = AstClause(loc.location, block, null)
        parseBlock(stmt)
        return stmt
    }

    private fun parseIf(block: AstBlock) {
        val clauses = mutableListOf<AstClause>()
        val loc = expect(IF)
        clauses += parseClause(block)
        while (canTake(ELSIF))
            clauses += parseClause(block)
        if (canTake(ELSE))
            clauses += parseElse(block)
        checkEnd(IF)
        block.add(AstIf(loc.location, clauses))
    }

    private fun parseStatement(block: AstBlock) {
        when(lookahead.kind) {
            VAR, VAL -> parseDecl(block)
            CONST -> parseConst(block)
            ID, OPENB -> parseAssign(block)
            WHILE -> parseWhile(block)
            IF -> parseIf(block)
            FUN -> parseFun(block)
            RETURN -> parseReturn(block)
            CLASS -> parseClass(block)
            FOR -> parseFor(block)
            REPEAT -> parseRepeat(block)
            ENUM -> parseEnum(block)
            else -> throw ParseError(lookahead.location,"Got '$lookahead' when expecting statement")
        }
    }

    fun parseTop(block:AstTop)  {
        while(lookahead.kind!=EOF) {
            try {
                parseStatement(block)
            } catch (e:ParseError) {
                Log.error(e.message!!)
                skipToEol()
            }
        }
        return
    }

}