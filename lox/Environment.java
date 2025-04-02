package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Environment {
    final Environment enclosing;

    // cambiamos esto, ahora indices es un map con cada variable como key y el index como la posicion ( {"a":0, "b":1} )
    private final Map<String, Integer> indices = new HashMap<>();
    // creamos esto, values es una lista con el valor de cada variable ( ["hola","chau"] ) --> entonces a seria "hola" y b seria "chau"
    private final List<Object> values = new ArrayList<>();
    
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

        // If the variable already exists, just update its value
        Integer index = indices.get(name);
        if(index != null){
            values.set(index,value);
        }else{
            // Otherwise, create a new variable with next index
            indices.put(name, values.size());
            values.add(value);
        }
        // values.put(name,value);
    }

    Environment ancestor(int distance){
        Environment environment = this;
        for(int i = 0; i < distance; i++){
            environment = environment.enclosing;
        }
        return environment;
    }

    Object getAt(int distance, int index){
        // challenge 4 pag 191
        //  hay que obtenerla por index en vez de name aca
        // return ancestor(distance).values.get(name);
        return ancestor(distance).values.get(index);
    }

    void assignAt(int distance, int index, Object value){
        // ancestor(distance).values.put(name.lexeme, value);
        ancestor(distance).values.set(index, value);
    }

    Object get(Token name){
        if(indices.containsKey(name.lexeme)){
            int index = indices.get(name.lexeme);
            // return values.get(name.lexeme);
            return values.get(index);
        }

        if(enclosing != null) return enclosing.get(name); //recursivamente, ya que llama a este mismo metodo get del otro environment

        throw new RuntimeError(name,"Undefined variable ' " + name.lexeme + " '.");
    }

    void assign(Token name,Object value){
        if(indices.containsKey(name.lexeme)){
            int index = indices.get(name.lexeme);
            values.set(index, value);
            // values.put(name.lexeme,value);
            
            return;
        }

        if(enclosing != null){
            enclosing.assign(name,value); //recursivamente, ya que llama a este mismo metodo assign del otro environment
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
