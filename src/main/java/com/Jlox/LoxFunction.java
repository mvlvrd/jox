package com.Jlox;

import java.util.List;

public class LoxFunction implements LoxCallable {

    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean IsInitializer;

    LoxFunction(Stmt.Function declaration, Environment closure, boolean IsInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.IsInitializer = IsInitializer;
    }

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this(declaration, closure, false);
    }

    LoxFunction bind(LoxInstance instance) {
        Environment env = new Environment(closure);
        env.define("this", instance);
        return new LoxFunction(declaration, env, IsInitializer);
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment env = new Environment(closure);
        for (int i = 0; i < arity(); i++) {
            env.define(declaration.params.get(i).lexeme, args.get(i));
        }
        try {
            interpreter.executeBlock(declaration.body, env);
        } catch (Return ret) {
            return IsInitializer ? closure.getAt(0, "this") : ret.value;
        }
        return IsInitializer ? closure.getAt(0, "this") : null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
