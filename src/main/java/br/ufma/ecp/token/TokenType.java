package br.ufma.ecp.token;


import java.util.List;
import java.util.Map;

public enum TokenType {
    PLUS,MINUS,

     // Literals.
     NUMBER,
     STRING,


     IDENT, EQ, SEMICOLON,


     PRINT,


    // keywords
    WHILE, CLASS,CONSTRUCTOR,FUNCTION,
    METHOD,FIELD,STATIC,VAR,INT,
    CHAR,BOOLEAN,VOID,TRUE,FALSE,
    NULL,THIS,LET,DO,IF,ELSE, RETURN,


     //simbolos
      LPAREN,RPAREN,
         LBRACE, RBRACE,
         LBRACKET,RBRACKET,

     COMMA, DOT,

     ASTERISK, SLASH,

     AND, OR, NOT,

     LT, GT, ILLEGAL,
     
     EOF;

 
     // keywords


     static public boolean isSymbol (String lexame) {
        String symbols = "{}()[].,;+-*/&|<>=~";
        return symbols.indexOf(lexame) > -1;
    }

    private TokenType() {
    }

    private TokenType(String value) {
        this.value = value;
    }

    public String value;


    static public boolean isKeyword (TokenType type) {
        List<TokenType> keywords  = 
            List.of(
                WHILE, CLASS,CONSTRUCTOR,FUNCTION,
                METHOD,FIELD,STATIC,VAR,INT,
                CHAR,BOOLEAN,VOID,TRUE,FALSE,
                NULL,THIS,LET,DO,IF,ELSE, RETURN
            );
            return keywords.contains(type);
    }
    
}