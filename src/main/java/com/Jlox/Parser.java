package com.Jlox;

import static com.Jlox.Jlox.MAX_ARITY;
import static com.Jlox.TokenType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
    }

    Expr parseSingleExpr() {
        return expression();
    }

    /*TODO: This only needs to be public for testing: Make package private?*/
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!EoF()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            else if (match(FUN)) return funDeclaration();
            else return statement();
        } catch (ParseError err) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name");
        Expr initializer = match(EQUAL) ? expression() : null;
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt funDeclaration() {
        Token name = consume(IDENTIFIER, "Expect function variable name");
        consume(LEFT_PAREN, "Expect opening paren '('");
        List<Token> parameters = parseParameters();
        consume(RIGHT_PAREN, "Expect closing paren ')'");
        consume(LEFT_BRACE, "Expect opening brace '{'");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    List<Token> parseParameters() {
        List<Token> params = new ArrayList<>();
        while (!check(RIGHT_PAREN)) {
            if (params.size() == MAX_ARITY)
                error(peek(), "Can't have more than " + MAX_ARITY + " arguments.");
            params.add(consume(IDENTIFIER, "Expect parameter name."));
            if (check(RIGHT_PAREN)) break;
            consume(COMMA, "Missing comma between parameters");
        }
        return params;
    }

    private Stmt statement() {
        try {
            if (match(PRINT)) return printStatement();
            if (match(LEFT_BRACE)) return new Stmt.Block(block());
            if (match(IF)) return ifStmt();
            if (match(WHILE)) return whileStmt();
            if (match(FOR)) return forStmt();
            if (match(RETURN)) return returnStmt();
            if (match(BREAK)) return breakStmt();
            return expressionStatement();
        } catch (ParseError err) {
            synchronize();
            return null;
        }
    }

    private Stmt breakStmt() {
        Token keyword = previous();
        Token name = check(SEMICOLON) ? null : consume(IDENTIFIER, "Expect loop name");
        consume(SEMICOLON, "Expect ';' after break.");
        return new Stmt.Break(keyword, name);
    }

    private Stmt returnStmt() {
        Token keyword = previous();
        Expr expr = check(SEMICOLON) ? null : expression();
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, expr);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private List<Stmt> block() {
        List<Stmt> stmts = new ArrayList<>();
        while (!(check(RIGHT_BRACE) || EoF())) stmts.add(declaration());
        consume(RIGHT_BRACE, "Expect closing brace '}'");
        return stmts;
    }

    private Stmt ifStmt() {
        consume(LEFT_PAREN, "Expect opening paren '('");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect closing paren ')'");
        Stmt ifBranch = statement();
        Stmt elseBranch = match(ELSE) ? statement() : null;
        return new Stmt.If(condition, ifBranch, elseBranch);
    }

    private Stmt whileStmt() {
        consume(LEFT_PAREN, "Expect opening paren '('");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect closing paren ')'");
        Stmt body = statement();
        return new Stmt.While(condition, body, null);
    }

    private Stmt forStmt() {
        consume(LEFT_PAREN, "Expect opening paren '('");
        Stmt init = null;
        if (match(VAR)) init = varDeclaration();
        else if (!match(SEMICOLON)) init = expressionStatement();
        Expr condition = check(SEMICOLON) ? null : expression();
        consume(SEMICOLON, "Expect ';' after loop condition.");
        Expr increment = check(RIGHT_PAREN) ? null : expression();
        consume(RIGHT_PAREN, "Expect closing paren ')'");
        Stmt body = statement();

        if (increment != null)
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body, null);
        if (init != null) body = new Stmt.Block(Arrays.asList(init, body));

        return body;
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Expression(value);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target");
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token op = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, op, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            Token op = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, op, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                List<Expr> args = finishCall();
                Token lastParen = consume(RIGHT_PAREN, "Expect ')' after arguments.");
                expr = new Expr.Call(expr, lastParen, args);
            } else break;
        }

        return expr;
    }

    private List<Expr> finishCall() {
        List<Expr> args = new ArrayList<>();
        while (!check(RIGHT_PAREN)) {
            if (args.size() == MAX_ARITY)
                error(peek(), "Can't have more than " + MAX_ARITY + " arguments.");
            args.add(expression());
            if (check(RIGHT_PAREN)) break;
            consume(COMMA, "Missing comma between arguments.");
        }
        return args;
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        else if (match(TRUE)) return new Expr.Literal(true);
        else if (match(NIL)) return new Expr.Literal(null);
        else if (match(FLOAT, INTEGER, STRING)) return new Expr.Literal(previous().literal);
        else if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        } else if (match(IDENTIFIER)) return new Expr.Variable(previous());
        throw error(peek(), ": Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        if (!EoF()) current++;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean check(TokenType type) {
        if (EoF()) return false;
        return peek().type == type;
    }

    private void synchronize() {
        advance();
        while (!EoF()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    private boolean EoF() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token consume(TokenType type, String msg) {
        if (check(type)) return advance();
        else throw error(peek(), msg);
    }

    private static ParseError error(Token token, String msg) {
        Jlox.error(token.line, msg);
        return new ParseError();
    }

    static class ParseError extends RuntimeException {}
}
