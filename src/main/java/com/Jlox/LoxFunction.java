package com.Jlox;

import java.util.List;

public class LoxFunction implements LoxCallable {

    Stmt.Function declaration;
    Environment closure;

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
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
        Object obj = null; // TODO: Should this be a nil Lox object?
        try {
            interpreter.executeBlock(declaration.body, env);
        } catch (Return ret) {
            obj = ret.value;
        }
        return obj;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + " >";
    }
}
