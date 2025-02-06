package com.Jlox;

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

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initFunc = findMethod("init");
        if (initFunc != null) initFunc.bind(instance).call(interpreter, args);
        return instance;
    }

    @Override
    public int arity() {
        LoxFunction initFunc = findMethod("init");
        return (initFunc == null) ? 0 : initFunc.arity();
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) return methods.get(name);
        else if (superClass != null) return superClass.findMethod(name);
        else return null;
    }
}
