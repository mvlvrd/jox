package com.Jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private LoopType currentLoop = LoopType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private static enum FunctionType {
        NONE,
        FUNCTION
    }

    private static enum LoopType {
        NONE,
        LOOP
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
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
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
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE)
            Jlox.error(stmt.keyword, "Can't return from top-level code.");
        if (stmt.value != null) resolve(stmt.value);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (currentLoop == LoopType.NONE)
            Jlox.error(stmt.keyword, "Can't return from top-level code.");
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
        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(token.lexeme))
            Jlox.error(token, "Variable already defined in this scope.");
        scope.put(token.lexeme, false);
    }

    void define(Token token) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(token.lexeme, true);
    }

    void beginScope() {
        scopes.push(new HashMap<>());
    }

    void finishScope() {
        scopes.pop();
    }
}
