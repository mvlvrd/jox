package com.Jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, VARSTATE>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private LoopType currentLoop = LoopType.NONE;
    private ClassType currentClass = ClassType.NONE;

    private enum VARSTATE {
        DECLARED,
        DEFINED,
        USED
    }

    private static enum FunctionType {
        NONE,
        FUNCTION,
        METHOD,
        INIT
    }

    private static enum LoopType {
        NONE,
        LOOP
    }

    private static enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    void resolve(List<Stmt> stmts) {
        for (Stmt stmt : stmts) resolve(stmt);
    }

    void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr) {
        expr.accept(this);
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expr);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == VARSTATE.DECLARED) {
            Jlox.error(expr.name, "Can't read local variable in its own initializer.");
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr arg : expr.args) {
            resolve(arg);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.obj);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expr);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expr);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) resolve(stmt.initializer);
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.stmts);
        finishScope();
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.thenBranch);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        LoopType previousLoop = currentLoop;
        currentLoop = LoopType.LOOP;
        resolve(stmt.condition);
        resolve(stmt.body);
        currentLoop = previousLoop;
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;
        declare(stmt.name);
        if (stmt.superClass!=null) {
            currentClass = ClassType.SUBCLASS;
            if (stmt.superClass.name.lexeme.equals(stmt.name.lexeme)) Jlox.error(stmt.superClass.name, "A class cannot inherit from itself.");
            resolve(stmt.superClass);
            beginScope();
            scopes.peek().put("super", VARSTATE.DEFINED);
        }
        beginScope();
        scopes.peek().put("this", VARSTATE.DEFINED);
        for (Stmt.Function func: stmt.methods) {
            resolveFunction(func, func.name.lexeme.equals("init") ? FunctionType.INIT : FunctionType.METHOD);
        }
        define(stmt.name);
        finishScope();
        if (stmt.superClass != null) finishScope();
        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.obj);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass != ClassType.CLASS) {
            Jlox.error(expr.keyword, "Can not use 'this' outside a class.");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            Jlox.error(expr.keyword, "Can not use 'super' outside a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            Jlox.error(expr.keyword, "Can not use 'super' in a base class.");
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE)
            Jlox.error(stmt.keyword, "Can't return from top-level code.");
        if (stmt.value != null) {
            if (currentFunction == FunctionType.INIT) Jlox.error(stmt.keyword, "Can't return a value from initializer.");
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (currentLoop == LoopType.NONE)
            Jlox.error(stmt.keyword, "Can't break outside while- or for- loop.");
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void resolveFunction(Stmt.Function stmt, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : stmt.params) {
            declare(param);
            define(param);
        }
        resolve(stmt.body);
        finishScope();
        currentFunction = enclosingFunction;
    }

    void declare(Token token) {
        if (scopes.isEmpty()) return;
        Map<String, VARSTATE> scope = scopes.peek();
        if (scope.containsKey(token.lexeme))
            Jlox.error(token, "Variable already defined in this scope.");
        scope.put(token.lexeme, VARSTATE.DECLARED);
    }

    void define(Token token) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(token.lexeme, VARSTATE.DEFINED);
    }

    void beginScope() {
        scopes.push(new HashMap<>());
    }

    void finishScope() {
        scopes.pop();
    }
}
