package com.Jlox;

import java.util.List;
import java.util.Objects;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment environment = new Environment();

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (truthy(evaluate(stmt.condition))) execute(stmt.thenBranch);
        else execute(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (truthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object val = evaluate(expr.value);
        environment.assign(expr.name, val);
        return val;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block block) {
        Environment previous = this.environment;
        try {
            this.environment = new Environment(previous);
            for (Stmt stmt : block.stmts) {
                execute(stmt);
            }
        } finally {
            this.environment = previous;
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object obj = stmt.initializer == null ? null : evaluate(stmt.initializer);
        environment.define(stmt.name.lexeme, obj);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object obj = evaluate(stmt.expr);
        System.out.println(makeString(obj));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expr);
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.op.type == TokenType.OR) {
            if (truthy(left)) return left;
        } else if (!truthy(left)) return left;
        return evaluate(expr.right);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object lobj = evaluate(expr.left);
        Object robj = evaluate(expr.right);
        PairTypes pairTypes = getPairTypes(lobj, robj);
        String ltype = pairTypes.ltype;
        String rtype = pairTypes.rtype;
        final boolean b = Objects.equals(ltype, "f") || Objects.equals(rtype, "f");
        final boolean ii = "i".equals(ltype) && "i".equals(rtype);
        return switch (expr.op.type) {
            case PLUS -> {
                if ("s".equals(ltype) || "s".equals(rtype)) yield (String) lobj + (String) robj;
                else if ("f".equals(ltype) && "f".equals(rtype))
                    yield (double) lobj + (double) robj;
                else if (ii) yield (int) lobj + (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
            }
            case MINUS -> b ? (double) lobj - (double) robj : (int) lobj - (int) robj;
            case EQUAL_EQUAL -> lobj.equals(robj);
            case LESS -> {
                if (b) yield (double) lobj < (double) robj;
                else if ("i".equals(ltype) && "i".equals(rtype)) yield (int) lobj < (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
            }
            case GREATER -> {
                if (b) yield (double) lobj > (double) robj;
                else if (ii) yield (int) lobj > (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
            }
            case LESS_EQUAL -> {
                if (b) yield (double) lobj <= (double) robj;
                else if (ii) yield (int) lobj <= (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
            }
            case GREATER_EQUAL -> {
                if (b) yield (double) lobj >= (double) robj;
                else if (ii) yield (int) lobj >= (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
            }
            default -> throw new RunTimeEvalError(expr.op, "Wrong binary operator.");
        };
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expr);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object obj = evaluate(expr.right);
        return switch (expr.op.type) {
            case MINUS -> {
                switch (getType(obj)) {
                    case "f" -> {
                        yield -(double) obj;
                    }
                    case "i" -> {
                        yield -(int) obj;
                    }
                    default -> throw new RunTimeEvalError(expr.op, "Operand must be a number.");
                }
            }
            case BANG -> !(boolean) obj;
            default -> throw new RunTimeEvalError(expr.op, "Not a unary operator.");
        };
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    void interpret(List<Stmt> stmts) {
        try {
            for (Stmt stmt : stmts) {
                execute(stmt);
            }
        } catch (RunTimeEvalError err) {
            Jlox.runTimeError(err);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void interpret(Expr expr) {
        try {
            Object obj = evaluate(expr);
            System.out.println(makeString(obj));
        } catch (RunTimeEvalError err) {
            Jlox.runTimeError(err);
        }
    }

    static String makeString(Object obj) {
        if (obj == null) return "nil";
        return obj.toString();
    }

    private String getType(Object obj) {
        return switch (obj) {
            case Double f -> "f";
            case Integer i -> "i";
            case String s -> "s";
            case Boolean b -> "b";
            default -> null;
        };
    }

    private PairTypes getPairTypes(Object lobj, Object robj) {
        String ltype = getType(lobj);
        String rtype = getType(robj);
        return new PairTypes(ltype, rtype);
    }

    private Boolean truthy(Object obj) {
        if (obj == null) return false;
        else if (obj instanceof Boolean) return (boolean) obj;
        return false; //TODO: Decide on truthiness.
    }

    static record PairTypes(String ltype, String rtype) {}
}
