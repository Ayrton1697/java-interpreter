package lox;

import jata.util.List;
import jata.util.Map;

public class LoxClass implements LoxCallable {
    final String name;

    LoxClass(String name){
        this.name = name;
    }

    @Override
    public String toString(){
        return name; 
    }

    @Override
    public Object call(Interpreter interpreter,
                    List<Object> arguments){
        
    }
}
