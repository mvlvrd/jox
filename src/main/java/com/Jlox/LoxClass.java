package com.Jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    private final String name;
    private final LoxClass superClass;
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, LoxClass superClass, Map<String, LoxFunction> methods) {
        this.name = name;
        this.superClass = superClass;
        this.methods = methods;
    }

    LoxClass(String name) {
        this.name = name;
        this.superClass = null;
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

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) return methods.get(name);
        else if (superClass != null) return superClass.findMethod(name);
        else return null;
    }
}
