package br.ufma.ecp;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();

    public Parser(byte[] input) {
        scan = new Scanner(input);
        nextToken();
        nextToken(); // Para inicializar peekToken
    }

    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }

    public void parse() {
        expr();
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
}
