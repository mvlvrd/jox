package com.Jlox;

class PairTypes {
    String ltype;
    String rtype;

    PairTypes(String ltype, String rtype) {
        this.ltype = ltype;
        this.rtype = rtype;
    }

    PairTypes(Object lobj, Object robj) {
        this(getType(lobj), getType(robj));
    }

    static String getType(Object obj) {
        return switch (obj) {
            case Double f -> "f";
            case Integer i -> "i";
            case String s -> "s";
            case Boolean b -> "b";
            default -> null;
        };
    }
}
