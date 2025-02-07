package lox;

import java.util.List;
import static lox.TokenType.*;

class Parser {
    private final List<Token> tokens;
    private int current = 0

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }
}