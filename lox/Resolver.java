package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import lox.Interpreter;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>{
    private final Interpreter interpreter;
    // private final Stack<Map<String,Boolean>> scopes = new Stack();
    
    // private final Stack<Map<String,Object[]>> scopes = new Stack();

    // para trackear si una variable se uso agregamos una propiedad
    // private final Stack<Map<String,Map<String,Boolean>>> scopesTwo= new Stack();
    // La idea es que se lea -> "variable a" -> "{defined:true, used:false}" 
    // Habria que cambiar como se crea la variable en el map y dsp cambiar el used a true cuando se resuelve esa variable
    

    // el resolver basicamente devuelve { VarExpr("a") -> 1}, si es que la variable "a" se encuentra a 1 scope de distancia. Esta info se la pasa
    // al interpreter en resolveLocal, y el interpreter terminando poniendo en su "locals" esto locals = {
    //     [Expr.Variable(a)] -> 1
    //   }


    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter){
        this.interpreter = interpreter;
    }

    private enum FunctionType{
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType{
        NONE,
        CLASS,
        SUBCLASS
    }

    private ClassType currentClass = ClassType.NONE;

    // @Override 
    // public Void visitFunctionExpr(Expr.Function expr){
    //     return null;
    // }

    @Override 
    public Void visitBlockStmt(Stmt.Block stmt){
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt){
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);
         if(stmt.superclass != null && stmt.name.lexeme.equals(stmt.superclass.name.lexeme)){
            Lox.error(stmt.superclass.name, "A class cant inherit from itself.");
        }
        if(stmt.superclass != null){
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);
        }
        if(stmt.superclass != null){
            beginScope();
            scopes.peek().put("super", true);
        }
        beginScope();
        scopes.peek().put("this", true);

        for(Stmt.Function method : stmt.methods){
            FunctionType declaration = FunctionType.METHOD;
            if(method.name.lexeme.equals("init")){
                declaration = FunctionType.INITIALIZER;
            }
            resolveFunction(method, declaration);
        }

        endScope();
        
        if(stmt.superclass !=null) endScope();

        currentClass = enclosingClass;

        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt){
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt){
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt){
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if(stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt){
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt){
        if(currentFunction == FunctionType.NONE){
            Lox.error(stmt.keyword, "Cant return from top level code.");
        }

        if(stmt.value != null){
            if(currentFunction == FunctionType.INITIALIZER){
                Lox.error(stmt.keyword, "Cant return a value from an initializer.");
            }
            resolve(stmt.value);
        }
        return null;
    }

    @Override 
    public Void visitVarStmt(Stmt.Var stmt){
        declare(stmt.name);
        if(stmt.initializer != null){
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt){
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr){
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr){
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr){
        resolve(expr.callee);
        for(Expr argument : expr.arguments){
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr){
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr){
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr){
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr){
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr){
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr){
        if(currentClass == ClassType.NONE){
            Lox.error(expr.keyword, "Cant user 'super' outside of a class");
        } else if (currentClass != ClassType.SUBCLASS){
            Lox.error(expr.keyword, "Cant user 'super' in a class with no superclass");
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr){
        if(currentClass == ClassType.NONE){
            Lox.error(expr.keyword, "Cant use 'this' outside of a class.");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr){
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        // No variables to resolve in a break statement, so just return null
        return null;
    }

    // challenge
    // @Override 
    // public Void visitVariableExpr(Expr.Variable expr) {
    //     if (!scopes.isEmpty()) {
    //         Map<String, Object[]> scope = scopes.peek();
    //         Object[] metadata = scope.get(expr.name.lexeme);
            
    //         if (metadata != null && (boolean)metadata[0] == false) {
    //             Lox.error(expr.name, "Can't read local variable in its own initializer.");
    //         }
    //     }
        
    //     resolveLocal(expr, expr.name);
    //     return null;
    // }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() &&
            scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
        Lox.error(expr.name,
            "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolve(Stmt stmt){
        stmt.accept(this);
    }

    private void resolveFunction(Stmt.Function function, FunctionType type){
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for(Token param:function.params){
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    private void resolve(Expr expr){
        expr.accept(this);
    }

    void resolve(List<Stmt> statements){
        for(Stmt statement:statements){
            resolve(statement);
        }
    }

    // private void beginScope(){
    //     scopes.push(new HashMap<String,Object[]>());
    // }

    private void beginScope() {
       scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope(){
        scopes.pop();
    }

    // challenge
    // private void declare(Token name){
    //     if (scopes.isEmpty()) return;
    //     Map<String, Object[]> scope = scopes.peek();
    //     if(scope.containsKey(name.lexeme)){
    //         Lox.error(name,
    //         "Already a variable with this name in this scope.");
    //     }
    //     // scope.put(name.lexeme, false);
    //     // asignar un index a la variable
    //     scope.put(name.lexeme, new Object[] {false, scope.size()} );
    // }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();
        //> duplicate-variable
        if (scope.containsKey(name.lexeme)) {
        Lox.error(name,
            "Already a variable with this name in this scope.");
        }

     //< duplicate-variable
        scope.put(name.lexeme, false);
    }

    // challenge
    // private void define(Token name){
    //     if (scopes.isEmpty()) return;
    //     // scopes.peek().put(name.lexeme,true);
    //     scopes.peek().get(name.lexeme)[0] = true;
    // }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    // challenge
    // private void resolveLocal(Expr expr, Token name){
    //     for(int i = scopes.size() - 1; i>=0; i--){
    //         // aca buscamos el scope donde esta la variable, para el challenge 4 pag 191 ternemos que buscar tambien el index que tenemos q haber agregado antes
    //         if(scopes.get(i).containsKey(name.lexeme)){
    //             int index = (int)scopes.get(i).get(name.lexeme)[1];
    //             interpreter.resolve(expr, scopes.size() - 1 -i, index);
    //             return;
    //         }
    //     }
    // }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
        if (scopes.get(i).containsKey(name.lexeme)) {
            interpreter.resolve(expr, scopes.size() - 1 - i);
            return;
        }
        }
    }
}