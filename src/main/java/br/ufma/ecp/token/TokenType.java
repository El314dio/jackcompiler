package br.ufma.ecp.token;
public enum TokenType {
    PLUS,MINUS,

     // Literals.
     NUMBER,
     STRING,


     IDENT,

     // keywords
     METHOD,
     WHILE,
     IF,

     EOF,

     ILLEGAL;

     static public boolean isSymbol (char c) {
        String symbols = "{}()[].,;+-*/&|<>=~";
        return symbols.indexOf(c) > -1;
    }

}
