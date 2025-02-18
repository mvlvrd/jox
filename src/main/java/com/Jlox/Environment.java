package com.Jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    protected final Environment enclosing;

    Environment() {
        this.enclosing = null;
    }

    Environment(Map<String, Object> initVals) {
        this(initVals, null);
    }

    Environment(Map<String, Object> initVals, Environment enclosing) {
        this.values.putAll(initVals);
        this.enclosing = enclosing;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        // TODO: This allows for variable redefinition. Perhaps it should raise a warning.
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) return values.get(name.lexeme);
        if (enclosing != null) return enclosing.get(name);
        throw new RunTimeEvalError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object val) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, val);
            return;
        } else if (enclosing != null) {
            enclosing.assign(name, val);
            return;
        }
        throw new RunTimeEvalError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    Object getAt(Integer distance, String name) {
        return ancestor(distance).values.get(name);
    }

    Environment ancestor(int distance) {
        Environment env = this;
        for (int i = 0; i < distance; i++) {
            env = env.enclosing;
        }
        return env;
    }
}
