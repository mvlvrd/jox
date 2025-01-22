package com.Jlox;

public class Break extends RuntimeException {
    final Token name;

    Break(Token name) {
        super(null, null, false, false);
        this.name = name;
    }
}
