package com.Jlox;

import java.util.List;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    String print(Stmt stmt) {
        return stmt.accept(this);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return parenthesize2("call", expr.callee, expr.args);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize(expr.name.lexeme, expr.value);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return parenthesize(expr.name.lexeme);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.op.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expr);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        else return expr.value.toString();
    }

    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.op.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.op.lexeme, expr.right);
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return parenthesize("Expression statement:", stmt.expr);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return parenthesize("Print statement:", stmt.expr);
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        return parenthesize("Var" + stmt.name.lexeme + " initialization:", stmt.initializer);
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        StringBuilder builder = new StringBuilder("(block ");
        for (Stmt statement : stmt.stmts) {
            builder.append(statement.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        return "";
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return "";
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        return "";
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        return "";
    }

    @Override
    public String visitBreakStmt(Stmt.Break stmt) {
        return "";
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ").append(expr.accept(this));
        }
        return builder.append(")").toString();
    }

    private String parenthesize2(String name, Object... parts) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        transform(builder, parts);
        builder.append(")");

        return builder.toString();
    }

    private void transform(StringBuilder builder, Object... parts) {
        for (Object part : parts) {
            builder.append(" ");
            switch (part) {
                case Expr expr -> builder.append(expr.accept(this));
                case Stmt stmt -> {
                    // builder.append(((Stmt) part).accept(this));
                    assert true;
                }
                case Token token -> builder.append(token.lexeme);
                case List list -> transform(builder, list.toArray());
                case null, default -> builder.append(part);
            }
        }
    }

    private String printStmtList(List<Stmt> stmts) {
        Iterable<String> stmtStr = () -> stmts.stream().map(this::print).iterator();
        return String.join("\n", stmtStr);
    }
}
