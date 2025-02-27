package br.ufma.ecp;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();

    private String className;


    public Parser(byte[] input) {
        scan = new Scanner(input);
        nextToken();
    }

    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }

    public void parse() {
        parseClass();
    }

    public void expr() {
        number();
        oper();
    }

    public void number() {
        xmlOutput.append(String.format("<number> %s </number>\r\n", currentToken.lexeme));
        match(TokenType.NUMBER);
    }

    private void match(TokenType t) {
        if (currentToken.type == t) {
            nextToken();
        } else {
            throw error(currentToken, "Syntax error: expected " + t.name());
        }
    }

    public void oper() {
        if (currentToken.type == TokenType.PLUS) {
            xmlOutput.append("<operator> + </operator>\r\n");
            match(TokenType.PLUS);
            number();
            xmlOutput.append("<operation> add </operation>\r\n");
            oper();
        } else if (currentToken.type == TokenType.MINUS) {
            xmlOutput.append("<operator> - </operator>\r\n");
            match(TokenType.MINUS);
            number();
            xmlOutput.append("<operation> sub </operation>\r\n");
            oper();
        } else if (currentToken.type == TokenType.EOF) {
            xmlOutput.append("<end> </end>\r\n");
        } else {
            throw error(currentToken, "Unexpected token: " + currentToken.lexeme);
        }
    }

    public String XMLOutput() {
        return xmlOutput.toString();
    }

    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }

    boolean peekTokenIs(TokenType type) {
        return peekToken.type == type;
    }

    boolean currentTokenIs(TokenType type) {
        return currentToken.type == type;
    }

    private void expectPeek(TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                expectPeek(type);
                return;
            }
        }
        throw error(peekToken, "Expected one of the specified tokens");
    }

    private void expectPeek(TokenType type) {
        if (peekToken.type == type) {
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        } else {
            throw error(peekToken, "Expected " + type.name());
        }
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
    }

    private ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
        return new ParseError();
    }

    public void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
            case INT:
                expectPeek(TokenType.INT);
                break;
            case NUMBER:
                expectPeek(TokenType.NUMBER);
                break;
            case STRING:
                expectPeek(TokenType.STRING);
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(TokenType.FALSE, TokenType.NULL, TokenType.TRUE);
                break;
            case THIS:
                expectPeek(TokenType.THIS);
                break;
            case IDENT:
                expectPeek(TokenType.IDENT);
                if (peekTokenIs(TokenType.LPAREN) || peekTokenIs(TokenType.DOT)) {
                    parseSubroutineCall();
                } else { // variavel comum ou array
                    if (peekTokenIs(TokenType.LBRACKET)) { // array
                        expectPeek(TokenType.LBRACKET);
                        parseExpression();
                        expectPeek(TokenType.RBRACKET);

                    }
                }
                break;
            case LPAREN:
                expectPeek(TokenType.LPAREN);
                parseExpression();
                expectPeek(TokenType.RPAREN);
                break;
            case MINUS:
            case NOT:
                expectPeek(TokenType.MINUS, TokenType.NOT);
                parseTerm();
                break;
            default:
                System.err.println(peekToken.type);
                throw error(peekToken, "term expected");
        }
        printNonTerminal("/term");
    }

    static public boolean isOperator(String op) {
        return op != "" && "+-*/<>=~&|".contains(op);
    }

    public void parseExpression() {
        printNonTerminal("expression");
        parseTerm();
        while (isOperator(peekToken.lexeme)) {
            expectPeek(peekToken.type);
            parseTerm();
        }
        printNonTerminal("/expression");
    }

    // letStatement -> 'let' identifier( '[' expression ']' )? '=' expression ';'
    public void parseLet() {

        var isArray = false;

        printNonTerminal("letStatement");
        expectPeek(TokenType.LET);
        expectPeek(TokenType.IDENT);

        if (peekTokenIs(TokenType.LBRACKET)) { // array
            expectPeek(TokenType.LBRACKET);
            parseExpression();
            expectPeek(TokenType.RBRACKET);
            isArray = true;
        }

        expectPeek(TokenType.EQ);
        parseExpression();

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/letStatement");
    }

    public int parseExpressionList() {
        printNonTerminal("expressionList");

        var nArgs = 0;

        if (!peekTokenIs(TokenType.RPAREN))
        {
            parseExpression();
            nArgs = 1;
        }

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            parseExpression();
            nArgs++;
        }

        printNonTerminal("/expressionList");
        return nArgs;
    }

    // subroutineCall -> subroutineName '(' expressionList ')' |
    // (className|varName)'.'subroutineName '('expressionList ')
    public void parseSubroutineCall() {
        var nArgs = 0;

        var ident = currentToken.value();
        var functionName = ident + ".";

        if (peekTokenIs(TokenType.LPAREN)) {

            expectPeek(TokenType.LPAREN);
            nArgs = parseExpressionList() + 1;
            expectPeek(TokenType.RPAREN);
            functionName = className + "." + ident;

        } else {
            expectPeek(TokenType.DOT);
            expectPeek(TokenType.IDENT);
            expectPeek(TokenType.LPAREN);
            nArgs += parseExpressionList();
            expectPeek(TokenType.RPAREN);
        }
    }

    // 'do' subroutineCall ';'
    public void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(TokenType.DO);
        expectPeek(TokenType.IDENT);
        parseSubroutineCall();
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/doStatement");
    }

     public void parseClass() {
        printNonTerminal("class");
        expectPeek(TokenType.CLASS);
        expectPeek(TokenType.IDENT);
        className = currentToken.value();
        expectPeek(TokenType.LBRACE);

        while (peekTokenIs(TokenType.STATIC) || peekTokenIs(TokenType.FIELD)) {
            parseClassVarDec();
        }

        while (peekTokenIs(TokenType.FUNCTION) || peekTokenIs(TokenType.CONSTRUCTOR) || peekTokenIs(TokenType.METHOD)) {
            parseSubroutineDec();
        }

        expectPeek(TokenType.RBRACE);

        printNonTerminal("/class");
    }

    // classVarDec → ( 'static' | 'field' ) type varName ( ',' varName)* ';'
    public void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(TokenType.FIELD, TokenType.STATIC);

        // 'int' | 'char' | 'boolean' | className
        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        expectPeek(TokenType.IDENT);

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);
        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/classVarDec");
    }
    
    public void parseSubroutineDec() {
        printNonTerminal("subroutineDec");     

        expectPeek(TokenType.CONSTRUCTOR, TokenType.FUNCTION, TokenType.METHOD);

        var subroutineType = currentToken.type;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(TokenType.VOID, TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        expectPeek(TokenType.IDENT);

        var functionName = className + "." + currentToken.value();

        expectPeek(TokenType.LPAREN);
        parseParameterList();
        expectPeek(TokenType.RPAREN);
        parseSubroutineBody(functionName, subroutineType);

        printNonTerminal("/subroutineDec");
    }

    public void parseParameterList() {
        printNonTerminal("parameterList");

        if (!peekTokenIs(TokenType.RPAREN)) // verifica se tem pelo menos uma expressao
        {
            expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
            expectPeek(TokenType.IDENT);

            while (peekTokenIs(TokenType.COMMA)) {
                expectPeek(TokenType.COMMA);
                expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
                expectPeek(TokenType.IDENT);
            }

        }

        printNonTerminal("/parameterList");
    }


    public void parseSubroutineBody(String functionName, TokenType subroutineType) {
        printNonTerminal("subroutineBody");
        expectPeek(TokenType.LBRACE);
        while (peekTokenIs(TokenType.VAR)) {
            parseVarDec();
        }
        parseStatements();
        expectPeek(TokenType.RBRACE);
        printNonTerminal("/subroutineBody");
    }

    public void parseVarDec(){
        printNonTerminal("varDec");
        expectPeek(TokenType.VAR);

        // 'int' | 'char' | 'boolean' | className
        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);

        expectPeek(TokenType.IDENT);

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);
        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/varDec");

    }

    public void parseIf() {
        printNonTerminal("ifStatement");

        expectPeek(TokenType.IF);
        expectPeek(TokenType.LPAREN);

        parseExpression();
        expectPeek(TokenType.RPAREN);
        expectPeek(TokenType.LBRACE);

        parseStatements();
        expectPeek(TokenType.RBRACE);

        if (peekTokenIs(TokenType.ELSE))
        {
            expectPeek(TokenType.ELSE);
            expectPeek(TokenType.LBRACE);

            parseStatements();
            expectPeek(TokenType.RBRACE);
        }

        printNonTerminal("/ifStatement");
    }

    public void parseWhile() {
        printNonTerminal("whileStatement");

        expectPeek(TokenType.WHILE);
        expectPeek(TokenType.LPAREN);
        parseExpression();

        expectPeek(TokenType.RPAREN);
        expectPeek(TokenType.LBRACE);
        parseStatements();

        expectPeek(TokenType.RBRACE);
        printNonTerminal("/whileStatement");
    }

    // ReturnStatement -> 'return' expression? ';'
    public void parseReturn() {
        printNonTerminal("returnStatement");
        expectPeek(TokenType.RETURN);
        if (!peekTokenIs(TokenType.SEMICOLON)) {
            parseExpression();
        }
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/returnStatement");
    }

    public void parseStatements() {
        printNonTerminal("statements");
        while (peekToken.type == TokenType.WHILE ||
                peekToken.type == TokenType.IF ||
                peekToken.type == TokenType.LET ||
                peekToken.type == TokenType.DO ||
                peekToken.type == TokenType.RETURN) {
            parseStatement();
        }

        printNonTerminal("/statements");
    }

    public void parseStatement() {
        switch (peekToken.type) {
            case LET:
                parseLet();
                break;
            case IF:
                parseIf();
                break;
            case WHILE:
                 parseWhile();
                break;
            case RETURN:
                 parseReturn();
                break;
            case DO:
                parseDo();
                break;
            default:
                throw error(peekToken, "Expected a statement");
        }
    }
}