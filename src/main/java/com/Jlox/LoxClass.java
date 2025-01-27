package com.Jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    private final String name;
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    LoxClass(String name) {
        this.name = name;
        this.methods = new HashMap<>();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        return new LoxInstance(this);
    }

    @Override
    public int arity() {
        return 0;
    }

    LoxFunction findMethod(Token name) {
        return methods.getOrDefault(name.lexeme, null);
    }
 }
