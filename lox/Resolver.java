package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>{
    private final Interpreter interpreter;

    Resolver(Interpreter interpreter){
        this.interpreter = interpreter;
    }

    @Override Void visitBlockStmt(Stmt.Block stmt){
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    private void resolve(Stmt stmt){
        stmt.accept(this);
    }

    private void resolve(Expr expr){
        expr.accept(this);
    }

    void resolve(List<Stmt> statements){
        for(Stmt statement:statements){
            resolve(statement);
        }
    }

    private void beginScope(){
        scopes.push(new HashMap<String,Boolean>());
    }
}