package lox;

import java.util.List;

public class LoxFunction implements LoxCallable{
    
    private final Stmt.Function declaration; // null for anonymous
    private final Expr.Function expression;  // null for named
    private final Environment closure;
    private final boolean isInitializer;

    // LoxFunction(Stmt.Function declaration, Environment closure){
    //     this.closure = closure;
    //     this.declaration = declaration;
    // }

    LoxFunction(Stmt.Function declaration, Expr.Function expression, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.expression = expression;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    LoxFunction bind(LoxInstance instance){
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment);
    }
    

    @Override
    public String toString(){
        return "<fn " + declaration.name.lexeme + ">";
    }

    @Override
    public int arity(){
        if (declaration != null) return declaration.params.size();
        return expression.parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments){
        Environment environment = new Environment(closure);
        List<Token> params = (declaration != null) ? declaration.params : expression.parameters;
        for(int i = 0; i < params.size(); i++){
            environment.define(params.get(i).lexeme, arguments.get(i));
        }
        try {
            interpreter.executeBlock((declaration != null) ? declaration.body : expression.body, environment);
        }   catch(Return returnValue){
            return returnValue.value;
        }
        return null;
    }
}
