package com.Jlox;

public class RunTimeEvalError extends RuntimeException {
    final Token token;

    public RunTimeEvalError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
