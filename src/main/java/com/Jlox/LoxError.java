package com.Jlox;

public class LoxError extends RuntimeException {
    final int exitCode;

    LoxError(int exitCode) {
        this.exitCode = exitCode;
    }
}
