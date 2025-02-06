package com.Jlox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private final LoxClass loxClass;
    private final Map<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass loxClass) {
        this.loxClass = loxClass;
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) return fields.get(name.lexeme);
        LoxFunction func = loxClass.findMethod(name.lexeme);
        if (func != null) return func.bind(this);
        throw new RunTimeEvalError(name, "Undefined property '" + name.lexeme + "'.");
    }

    @Override
    public String toString() {
        return loxClass.toString() + " instance.";
    }

    public void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
}
