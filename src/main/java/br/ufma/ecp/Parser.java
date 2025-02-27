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
    }

    void expr() {
        number();
        oper();
    }

    void number() {
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

    void oper() {
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

    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
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
                break;
            default:
                throw error(peekToken, "term expected");
        }

        printNonTerminal("/term");
    }

    static public boolean isOperator(String op) {
        return op != "" && "+-*/<>=~&|".contains(op);
    }

    void parseExpression() {
        printNonTerminal("expression");
        parseTerm();
        while (isOperator(peekToken.lexeme)) {
            expectPeek(peekToken.type);
            parseTerm();
        }
        printNonTerminal("/expression");
    }

    void parseLet() {
        printNonTerminal("letStatement");
        expectPeek(TokenType.LET);
        expectPeek(TokenType.IDENT);

        if (peekTokenIs(TokenType.LBRACKET)) {
            expectPeek(TokenType.LBRACKET);
            parseExpression();
            expectPeek(TokenType.RBRACKET);
        }

        expectPeek(TokenType.EQ);
        parseExpression();
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/letStatement");


    }

    //identifier '(' ')'
    void parseSubroutineCall() {
       expectPeek(TokenType.IDENT);
       expectPeek(TokenType.LPAREN);
       expectPeek(TokenType.RPAREN);
    }

     
     // 'do' subroutineCall ';'
     void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(TokenType.DO);
        parseSubroutineCall();
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/doStatement");
     }

         // classVarDec → ( 'static' | 'field' ) type varName ( ',' varName)* ';'
    void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(TokenType.FIELD, TokenType.STATIC);

        // 'int' | 'char' | 'boolean' | className
        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        String type = currentToken.value();

        expectPeek(TokenType.IDENT);
        String name = currentToken.value();

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);

            name = currentToken.value();
        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/classVarDec");
    }

     void parseParameterList() {
        printNonTerminal("parameterList");

        if (!peekTokenIs(TokenType.RPAREN)) // verifica se tem pelo menos uma expressao
        {
            expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
            String type = currentToken.value();

            expectPeek(TokenType.IDENT);
            String name = currentToken.value();

            while (peekTokenIs(TokenType.COMMA)) {
                expectPeek(TokenType.COMMA);
                expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
                type = currentToken.value();

                expectPeek(TokenType.IDENT);
                name = currentToken.value();
            }

        }

        printNonTerminal("/parameterList");
    }


    public void parseSubroutineBody(String functionName, TokenType subroutineType) {

        printNonTerminal("subroutineBody");
        //expectPeek(TokenType.LBRACE);

       // expectPeek(TokenType.RBRACE);
        printNonTerminal("/subroutineBody");
    }


}