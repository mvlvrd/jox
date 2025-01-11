package com.Jlox;

import static com.Jlox.TokenType.*;

import java.util.List;

class Parser {
  private final List<Token> tokens;
  private int current;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
    this.current = 0;
  }

  Expr parse() {
    try {
      return expression();
    } catch (ParseError err) {
      return null;
    }
  }

  private Expr expression() {
    return equality();
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
    return primary();
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
    }
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
