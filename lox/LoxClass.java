package lox;

import jata.util.List;
import jata.util.Map;

public class LoxClass implements LoxCallable {
    final String name;

    private final java.util.Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods){
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(String name){
        if(methods.containsKey(name)){
            return methods.get(name);
        }
        return null;
    }

    @Override
    public String toString(){
        return name; 
    }

    @Override
    public Object call(Interpreter interpreter,
                    List<Object> arguments){
        LoxInstance instance = new LoxInstance(this);
        return instance;
    }

    @Override
    public int arity(){
        return 0;
    }
}
