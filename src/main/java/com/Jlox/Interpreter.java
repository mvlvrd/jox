package com.Jlox;

import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;

    void interpret(Expr expr) {
        try {
            Object obj = evaluate(expr);
            System.out.println(makeString(obj));
        } catch (RunTimeEvalError err) {
            Jlox.runTimeError(err);
        }
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

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> stmts, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt stmt : stmts) {
                execute(stmt);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (truthy(evaluate(stmt.condition))) execute(stmt.thenBranch);
        else if (stmt.elseBranch != null) execute(stmt.elseBranch);
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
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object obj = stmt.value == null ? null : evaluate(stmt.value);
        throw new Return(obj);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object val = evaluate(expr.value);
        environment.assign(expr.name, val);
        return val;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        if (!(callee instanceof LoxCallable function)) {
            throw new RunTimeEvalError(expr.lastParen, "Expression not callable.");
        }
        List<Expr> args = expr.args;
        if (function.arity() != args.size()) {
            throw new RunTimeEvalError(
                    expr.lastParen,
                    "Expected " + function.arity() + " arguments but got " + args.size());
        }
        List<Object> argVals = new ArrayList<>();
        for (Expr arg : args) {
            argVals.add(evaluate(arg));
        }
        return function.call(this, argVals);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block block) {
        executeBlock(block.stmts, new Environment(this.environment));
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
        PairTypes pairTypes = new PairTypes(lobj, robj);
        String ltype = pairTypes.ltype;
        String rtype = pairTypes.rtype;
        return switch (expr.op.type) {
            case PLUS -> {
                Object object;
                if (ltype.equals("f") && rtype == "f") object = (double) lobj + (double) robj;
                else if (ltype == "f" && rtype == "i") object = (double) lobj + (int) robj;
                else if (ltype == "i" && rtype == "f") object = (int) lobj + (double) robj;
                else if (ltype == "i" && rtype == "i") object = (int) lobj + (int) robj;
                else if (ltype == "s" && rtype == "s") object = lobj + (String) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
                yield object;
            }
            case MINUS -> {
                Object object;
                if (ltype == "f" && rtype == "f") object = (double) lobj - (double) robj;
                else if (ltype == "f" && rtype == "i") object = (double) lobj - (int) robj;
                else if (ltype == "i" && rtype == "f") object = (int) lobj - (double) robj;
                else if (ltype == "i" && rtype == "i") object = (int) lobj - (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
                yield object;
            }
            case EQUAL_EQUAL -> lobj.equals(robj);
            case LESS -> {
                Object object;
                if (ltype == "f" && rtype == "f") object = (double) lobj < (double) robj;
                else if (ltype == "f" && rtype == "i") object = (double) lobj < (int) robj;
                else if (ltype == "i" && rtype == "f") object = (int) lobj < (double) robj;
                else if (ltype == "i" && rtype == "i") object = (int) lobj < (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
                yield object;
            }
            case GREATER -> {
                Object object;
                if (ltype == "f" && rtype == "f") object = (double) lobj > (double) robj;
                else if (ltype == "f" && rtype == "i") object = (double) lobj > (int) robj;
                else if (ltype == "i" && rtype == "f") object = (int) lobj > (double) robj;
                else if (ltype == "i" && rtype == "i") object = (int) lobj > (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
                yield object;
            }
            case LESS_EQUAL -> {
                Object object;
                if (ltype == "f" && rtype == "f") object = (double) lobj <= (double) robj;
                else if (ltype == "f" && rtype == "i") object = (double) lobj <= (int) robj;
                else if (ltype == "i" && rtype == "f") object = (int) lobj <= (double) robj;
                else if (ltype == "i" && rtype == "i") object = (int) lobj <= (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
                yield object;
            }
            case GREATER_EQUAL -> {
                Object object;
                if (ltype == "f" && rtype == "f") object = (double) lobj >= (double) robj;
                else if (ltype == "f" && rtype == "i") object = (double) lobj >= (int) robj;
                else if (ltype == "i" && rtype == "f") object = (int) lobj >= (double) robj;
                else if (ltype == "i" && rtype == "i") object = (int) lobj >= (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
                yield object;
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
                switch (PairTypes.getType(obj)) {
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

    static String makeString(Object obj) {
        if (obj == null) return "nil";
        return obj.toString();
    }

    private Boolean truthy(Object obj) {
        if (obj == null) return false;
        else if (obj instanceof Boolean) return (boolean) obj;
        return false; // TODO: Decide on truthiness.
    }
}
