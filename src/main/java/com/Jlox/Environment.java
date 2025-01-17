package com.Jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Map<String, Object> values = new HashMap<>();
    private final Environment enclosing;

    Environment() {
        this.enclosing = null;
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
        throw new RunTimeEvalError(name, "Undefined variable:'" + name.lexeme + "'.");
    }

    void assign(Token name, Object val) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, val);
            return;
        } else if (enclosing != null) {
            enclosing.assign(name, val);
            return;
        }
        throw new RunTimeEvalError(name, "Undefined variable: '" + name.lexeme + "'.");
    }
}
