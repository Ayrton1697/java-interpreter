package lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass){
        this.klass = klass;
    }

    Object get(Token name){
        if(fields.containsKey(name.lexme)){
            return fields.get(name.lexeme);
        }

        LoxFunction method = klass.findMethod(name.lexeme);
        
        if(method != null) return method;
        
        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'. ");
    }

    Object set(Token name, Object value){
        fields.put(name,value);
    }

    @Override
    public String toString(){
        return klass.name + " instance";
    }
}
