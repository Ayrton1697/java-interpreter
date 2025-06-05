package lox;

import java.util.List;

public class LoxFunction implements LoxCallable{
    
    private final Stmt.Function declaration; // null for anonymous
    // private final Expr.Function expression;  // null for named
    private final Environment closure;
    private final boolean isInitializer;

    // LoxFunction(Stmt.Function declaration, Environment closure){
    //     this.closure = closure;
    //     this.declaration = declaration;
    // }

    LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.declaration = declaration;
    this.closure = closure;
    this.isInitializer = isInitializer;
    }

    // LoxFunction(Stmt.Function declaration, Expr.Function expression, Environment closure, boolean isInitializer) {
    //     this.declaration = declaration;
    //     this.expression = expression;
    //     this.closure = closure;
    //     this.isInitializer = isInitializer;
    // }

    LoxFunction bind(LoxInstance instance){
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }
    

    @Override
    public String toString(){
        return "<fn " + declaration.name.lexeme + ">";
    }

    // @Override
    // public int arity(){
    //     if (declaration != null) return declaration.params.size();
    //     return expression.parameters.size();
    // }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    // @Override
    // public Object call(Interpreter interpreter, List<Object> arguments){
    //     Environment environment = new Environment(closure);
    //     List<Token> params = (declaration != null) ? declaration.params : expression.parameters;
    //     for(int i = 0; i < params.size(); i++){
    //         environment.define(params.get(i).lexeme, arguments.get(i));
    //     }
    //     try {
    //         interpreter.executeBlock((declaration != null) ? declaration.body : expression.body, environment);
    //     }   catch(Return returnValue){
    //         if(isInitializer) return closure.getAt(0, "this");
    //         return returnValue.value;
    //     }
    //     if(isInitializer) return closure.getAt(0, "this");
    //     return null;
    // }

    @Override
    public Object call(Interpreter interpreter,
                     List<Object> arguments) {
        /* Functions function-call < Functions call-closure
            Environment environment = new Environment(interpreter.globals);
        */
        //> call-closure
            Environment environment = new Environment(closure);
        //< call-closure
            for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme,
                arguments.get(i));
            }

        /* Functions function-call < Functions catch-return
            interpreter.executeBlock(declaration.body, environment);
        */
        //> catch-return
            try {
            interpreter.executeBlock(declaration.body, environment);
            } catch (Return returnValue) {
        //> Classes early-return-this
            if (isInitializer) return closure.getAt(0, "this");

        //< Classes early-return-this
            return returnValue.value;
            }
        //< catch-return
        //> Classes return-this

            if (isInitializer) return closure.getAt(0, "this");
        //< Classes return-this
            return null;
        }

}
