package lox;

class Interpreter implements Expr.Visitor<Object> {

    @Override
    public Object visitLiteralExpr(Expr.Literal expr){
        return expr.value;
    }

    @Overide
    public Object visitUnaryExpr(Expr.Unary expr){
        
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr){
        return evaluate(expr.expression);
    }

    // Se manda recursivamente la expresion hacia el metodo correspondiente
    private Object evaluate(Expr expr){
        return expr.accept(this);
    }
}