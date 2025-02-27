package br.ufma.ecp.token;

import br.ufma.ecp.token.TokenType;

public class Token {

    public final TokenType type;
    public final String lexeme;
    public final int line;

    public Token (TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    public String value () {
        return type.value;
    }

    public String toString() {
        String categoria = type.toString().toLowerCase();
        String valor = lexeme;
        if (TokenType.isSymbol(lexeme)) {
            categoria = "symbol";
            if (valor.equals(">")) {
                valor = "&gt;" ;
            } else if (valor.equals("<")) {
                valor = "&lt;" ;
            } else if (valor.equals("\"")) {
                valor = "&quot;" ;
            } else if (valor.equals("&")) {
                valor = "&amp;" ;
            }

        } else if (categoria.equals("number")) {
            categoria = "integerConstant";
        } else if (categoria.equals("ident")) {
            categoria = "identifier";
        } else if (categoria.equals("string")) {
            categoria = "stringConstant";
        } else {
          categoria = "keyword";
        }
        return "<" + categoria + "> " + valor  + " </" + categoria + ">";
  

    }
    
}