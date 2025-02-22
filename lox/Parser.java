package lox;

import java.util.ArrayList;
import java.util.List;
import static lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    // Expr parse(){
    //     try {
    //         return expression();
    //     }   catch(ParseError error) {
    //         return null;
    //     }
    // }

    List<Stmt> parse(){
        List<Stmt> statements = new ArrayList<>();
        while(!isAtEnd()){
            statements.add(declaration());
        }

        return statements;
    }


    private Expr expression(){
        // return equality();
        // return comma();
        return assignment();
    }

    private Stmt declaration(){
        try{
            if(match(VAR)) return varDeclaration();
            return statement();
        }catch (ParseError error){
            synchronize();
            return null;
        }
    }

    private Stmt statement(){
        if(match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt printStatement(){
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration(){
        // var something = "asd";
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if(match(EQUAL)){
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement(){
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(value);
    }

    private List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();

        while(!check(RIGTH_BRACE) && !isAtEnd()){
            statements.add(declaration());
        }
        consume(RIGTH_BRACE, "Expect '}' after block");
        return statements;
    }

    private Expr assignment(){
        Expr expr = equality();
        if(match(EQUAL)){
            Token equals = previous();
            Expr value = assignment();

            if(expr instanceof Expr.Variable){
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }



    
    private Expr ternary(){
        // gramar rule es ternary -> expression "?" ternary ":" ternary | expression ;
        //  let result = a == 2 ? 3 : 4
        Expr expr = expression();
        if(match(QUESTION)){
            Expr exprThen = ternary();
            consume(COLON, "Expect :");
            Expr exprElse = ternary();
            return new Expr.Ternary(expr, exprThen, exprElse);
        }
        return expr;
    }

    // para agregar block statement support agregamos
    //  expression  → comma ;
    //  comma       → equality ( "," equality )* ;
    // let result = (a+=2, b+=3)
    private Expr comma(){
        Expr expr = equality();
        while(match(COMMA)){
            Token operator = previous(); //seria la coma que acabamos de consumir

            // inciso 3.'a'
            // binary operator appearing without left hand operand ---> (   , 5+=2; )
            // if (expr == null) error(operator, "Binary operator without left-hand operand."); 

            // Con esto descartamos y seguimos (inciso 3.'b'), ya que solo reportamos el error, en vez de raisear una exception
            if (expr == null) Lox.error(operator, "Binary operator without left-hand operand.");

            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr equality(){
        Expr expr = comparison();
        while(match(BANG_EQUAL, EQUAL_EQUAL)){
            Token operator = previous();

            // inciso 3.'a'
            // binary operator appearing without left hand operand ----> ( == 2; )
            // if (expr == null) error(operator, "Binary operator without left-hand operand."); 

            // (inciso 3.'b') Con esto descartamos y seguimos, ya que solo reportamos el error, en vez de raisear una exception
            if (expr == null) Lox.error(operator, "Binary operator without left-hand operand.");

            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison(){
        Expr expr = term();
        while(match(GREATER,GREATER_EQUAL,LESS,LESS_EQUAL)){
            
            Token operator = previous();

            // inciso 3.'a'
            // binary operator appearing without left hand operand ---> ( >= 2;)
            // if (expr == null) error(operator, "Binary operator without left-hand operand."); 

            // (inciso 3.'b') Con esto descartamos y seguimos, ya que solo reportamos el error, en vez de raisear una exception
            if (expr == null) Lox.error(operator, "Binary operator without left-hand operand.");
            
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term(){
        Expr expr = factor();
        while(match(MINUS,PLUS)){
            Token operator = previous();

            // inciso 3.'a'
            // binary operator appearing without left hand operand ---> ( + 1;)
            // if (expr == null) error(operator, "Binary operator without left-hand operand."); 

            // (inciso 3.'b') Con esto descartamos y seguimos, ya que solo reportamos el error, en vez de raisear una exception
            if (expr == null) Lox.error(operator, "Binary operator without left-hand operand.");

            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor(){
        Expr expr = unary();
        while(match(SLASH,STAR)){
            Token operator = previous();

            // inciso 3.'a'
            // binary operator appearing without left hand operand --> (  * 2; )
            // if (expr == null) error(operator, "Binary operator without left-hand operand."); 

            // (inciso 3.'b') Con esto descartamos y seguimos, ya que solo reportamos el error, en vez de raisear una exception
            if (expr == null) Lox.error(operator, "Binary operator without left-hand operand.");

            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary(){
        if(match(BANG,MINUS)){
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary(){
        if(match(TRUE)) return new Expr.Literal(TRUE);
        if(match(FALSE)) return new Expr.Literal(TRUE);
        if(match(NIL)) return new Expr.Literal(NIL);
        if(match(NUMBER,STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if(match(IDENTIFIER)){
            return new Expr.Variable(previous());
        }

        if(match(LEFT_PAREN)){
            Expr expr = expression();
            consume(RIGHT_PAREN,"Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(),"Expect expression.");
    }


    private boolean match(TokenType... types){
        for (TokenType type:types){
            if(check(type)){
                advance();
                return true;
            }
        }
            return false;
    }

    private Token consume(TokenType type, String message){
        if(check(type)) return advance();
        throw error(peek(), message);
    }


    
    private boolean check(TokenType type){
        if(isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance(){
        if(!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd(){
    	return peek().type == EOF;
    }

    private Token peek(){
	return tokens.get(current);	
    }

    private Token previous(){
    	return tokens.get(current-1);
    }

    private ParseError error(Token token,String message){
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize(){
        advance();
        while(!isAtEnd()){
            if(previous().type ==  SEMICOLON) return;

            switch (peek().type) {
                case CLASS: case FOR: case FUN: case IF: case PRINT:
                case RETURN: case VAR: case WHILE:
                    return;
            }
            advance();
        }
    }


    // CHALLENGE 1 PAG 133
    // public Expr parseExpression() {
    //     try {
    //         return expression(); // Leverage the private expression() method
    //     } catch (ParseError error) {
    //         return null; // Return null on error (e.g., invalid expression)
    //     }
    // }
    

}
