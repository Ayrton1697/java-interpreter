package lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();
    
    // CHALLENGE 2 PAG 133
    static final Object UNINITIALIZED = new Object();

    Environment(){
        enclosing = null;
    }

    Environment(Environment enclosing){
        this.enclosing = enclosing;
    }
    
    void define(String name, Object value){
        // CHALLENGE 2 PAG 133
        // values.put(name, value == null ? UNINITIALIZED : value);
        
        values.put(name,value);
    }

    Environment ancestor(int distance){
        Environment environment = this;
        for(int i = 0; i < distance; i++){
            environment = environment.enclosing;
        }
        return environment;
    }

    Object getAt(int distance, String name){
        return ancestor(distance).values.get(name);
    }

    Object get(Token name){
        if(values.containsKey(name.lexeme)){
            return values.get(name.lexeme);
        }

        if(enclosing != null) return enclosing.get(name); //recursivamente, ya que llama a este mismo metodo get del otro environment

        throw new RuntimeError(name,"Undefined variable ' " + name.lexeme + " '.");
    }

    void assign(Token name,Object value){
        if(values.containsKey(name.lexeme)){
            values.put(name.lexeme,value);
            return;
        }

        if(enclosing != null){
            enclosing.assign(name,value); //recursivamente, ya que llama a este mismo metodo assign del otro environment
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
