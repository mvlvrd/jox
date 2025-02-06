package com.Jlox;

import static java.util.Map.entry;

import com.Jlox.NativeFunctions.Clock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals =
            new Environment(new HashMap<>(Map.ofEntries(entry("clock", new Clock()))));

    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

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
        Object obj;
        try {
            obj = expr.accept(this);
        } catch (ArithmeticException exception) {
            obj = Float.NaN;
        }
        return obj;
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
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
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);

        Object superClass = null;
        if (stmt.superClass != null) {
            superClass = evaluate(stmt.superClass);
            if (!(superClass instanceof LoxClass))
                throw new RunTimeEvalError(stmt.name, "Superclass must be a class.");
            environment = new Environment(Map.of("super", superClass), environment);
        }

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            boolean IsInitializer = method.name.lexeme.equals("init");
            methods.put(method.name.lexeme, new LoxFunction(method, environment, IsInitializer));
        }
        LoxClass loxClass = new LoxClass(stmt.name.lexeme, (LoxClass) superClass, methods);
        if (superClass != null) environment = environment.enclosing;
        environment.assign(stmt.name, loxClass);
        return null;
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
        try {
            while (truthy(evaluate(stmt.condition))) {
                execute(stmt.body);
            }
        } catch (Break breakException) {
            if (!((breakException.name == null && stmt.name == null)
                    || Objects.equals(breakException.name.lexeme, stmt.name.lexeme)))
                throw breakException;
        }
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object obj = stmt.value == null ? null : evaluate(stmt.value);
        throw new Return(obj);
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new Break(stmt.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object val = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, val);
        } else {
            globals.assign(expr.name, val);
        }
        return val;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        if (!(callee instanceof LoxCallable function)) {
            throw new RunTimeEvalError(expr.lastParen, "Can only call functions and classes.");
        }
        List<Expr> args = expr.args;
        if (function.arity() != args.size()) {
            throw new RunTimeEvalError(
                    expr.lastParen,
                    "Expected " + function.arity() + " arguments but got " + args.size() + ".");
        }
        List<Object> argVals = new ArrayList<>();
        for (Expr arg : args) {
            argVals.add(evaluate(arg));
        }
        return function.call(this, argVals);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object obj = evaluate(expr.obj);
        if (obj instanceof LoxInstance inst) {
            return inst.get(expr.name);
        }
        throw new RunTimeEvalError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object obj = evaluate(expr.obj);
        if (!(obj instanceof LoxInstance inst))
            throw new RunTimeEvalError(expr.name, "Only instances have fields.");
        Object value = evaluate(expr.value);
        inst.set(expr.name, value);
        return value;
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
        return lookUpVariable(expr.name, expr);
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
                if (ltype == null || rtype == null)
                    throw new RunTimeEvalError(
                            expr.op, "Operands must be two numbers or two strings.");
                else if (ltype.equals("f") && rtype.equals("f"))
                    object = (double) lobj + (double) robj;
                else if (ltype.equals("f") && rtype.equals("i"))
                    object = (double) lobj + (int) robj;
                else if (ltype.equals("i") && rtype.equals("f"))
                    object = (int) lobj + (double) robj;
                else if (ltype.equals("i") && rtype.equals("i")) object = (int) lobj + (int) robj;
                else if (ltype.equals("s") && rtype.equals("s")) object = lobj + (String) robj;
                else
                    throw new RunTimeEvalError(
                            expr.op, "Operands must be two numbers or two strings.");
                yield object;
            }
            case STAR -> {
                Object object;
                if (ltype == null || rtype == null)
                    throw new RunTimeEvalError(expr.op, "Operands must be numbers.");
                else if (ltype.equals("f") && rtype.equals("f"))
                    object = (double) lobj * (double) robj;
                else if (ltype.equals("f") && rtype.equals("i"))
                    object = (double) lobj * (int) robj;
                else if (ltype.equals("i") && rtype.equals("f"))
                    object = (int) lobj * (double) robj;
                else if (ltype.equals("i") && rtype.equals("i")) object = (int) lobj * (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operands must be numbers.");
                yield object;
            }
            case SLASH -> {
                Object object;
                if (ltype == null || rtype == null)
                    throw new RunTimeEvalError(expr.op, "Operands must be numbers.");
                else if (ltype.equals("f") && rtype.equals("f"))
                    object = (double) lobj / (double) robj;
                else if (ltype.equals("f") && rtype.equals("i"))
                    object = (double) lobj / (int) robj;
                else if (ltype.equals("i") && rtype.equals("f"))
                    object = (int) lobj / (double) robj;
                else if (ltype.equals("i") && rtype.equals("i")) object = (int) lobj / (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operands must be numbers.");
                yield object;
            }
            case MINUS -> {
                Object object;
                if (ltype.equals("f") && rtype.equals("f")) object = (double) lobj - (double) robj;
                else if (ltype.equals("f") && rtype.equals("i"))
                    object = (double) lobj - (int) robj;
                else if (ltype.equals("i") && rtype.equals("f"))
                    object = (int) lobj - (double) robj;
                else if (ltype.equals("i") && rtype.equals("i")) object = (int) lobj - (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operands must be numbers.");
                yield object;
            }
            case EQUAL_EQUAL -> Objects.equals(lobj, robj);
            case LESS -> {
                Object object;
                if (ltype.equals("f") && rtype.equals("f")) object = (double) lobj < (double) robj;
                else if (ltype.equals("f") && rtype.equals("i"))
                    object = (double) lobj < (int) robj;
                else if (ltype.equals("i") && rtype.equals("f"))
                    object = (int) lobj < (double) robj;
                else if (ltype.equals("i") && rtype.equals("i")) object = (int) lobj < (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operands must be numbers.");
                yield object;
            }
            case GREATER -> {
                Object object;
                if (ltype.equals("f") && rtype.equals("f")) object = (double) lobj > (double) robj;
                else if (ltype.equals("f") && rtype.equals("i"))
                    object = (double) lobj > (int) robj;
                else if (ltype.equals("i") && rtype.equals("f"))
                    object = (int) lobj > (double) robj;
                else if (ltype.equals("i") && rtype.equals("i")) object = (int) lobj > (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operands must be numbers.");
                yield object;
            }
            case LESS_EQUAL -> {
                Object object;
                if (ltype.equals("f") && rtype.equals("f")) object = (double) lobj <= (double) robj;
                else if (ltype.equals("f") && rtype.equals("i"))
                    object = (double) lobj <= (int) robj;
                else if (ltype.equals("i") && rtype.equals("f"))
                    object = (int) lobj <= (double) robj;
                else if (ltype.equals("i") && rtype.equals("i")) object = (int) lobj <= (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operands must be numbers.");
                yield object;
            }
            case GREATER_EQUAL -> {
                Object object;
                if (ltype.equals("f") && rtype.equals("f")) object = (double) lobj >= (double) robj;
                else if (ltype.equals("f") && rtype.equals("i"))
                    object = (double) lobj >= (int) robj;
                else if (ltype.equals("i") && rtype.equals("f"))
                    object = (int) lobj >= (double) robj;
                else if (ltype.equals("i") && rtype.equals("i")) object = (int) lobj >= (int) robj;
                else throw new RunTimeEvalError(expr.op, "Operands must be numbers.");
                yield object;
            }
            case BANG_EQUAL -> !Objects.equals(lobj, robj);
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
            case BANG -> !truthy(obj);
            default -> throw new RunTimeEvalError(expr.op, "Not a unary operator.");
        };
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        LoxClass superClass = (LoxClass) environment.getAt(distance, "super");
        LoxInstance obj = (LoxInstance) environment.getAt(distance - 1, "this");
        LoxFunction func = superClass.findMethod(expr.method.lexeme);
        if (func == null)
            throw new RunTimeEvalError(
                    expr.keyword, "Undefined property '" + expr.method.lexeme + "'.");
        return func.bind(obj);
    }

    static String makeString(Object obj) {
        if (obj == null) return "nil";
        return obj.toString();
    }

    private Boolean truthy(Object obj) {
        if (obj == null) return false;
        else if (obj instanceof Boolean objB) return objB;
        return true;
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        Object obj =
                distance == null ? globals.get(name) : environment.getAt(distance, name.lexeme);
        return obj;
    }
}
