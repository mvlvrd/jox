package com.Jlox;

import java.util.Objects;

class Interpreter implements Expr.Visitor<Object> {

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object lobj = evaluate(expr.left);
    Object robj = evaluate(expr.right);
    PairTypes pairTypes = getPairTypes(lobj, robj);
    String ltype = pairTypes.ltype;
    String rtype = pairTypes.rtype;
    return switch (expr.op.type) {
      case PLUS -> {
        if (Objects.equals(ltype, "s") || Objects.equals(rtype, "s"))
          yield (String) lobj + (String) robj;
        else if (Objects.equals(ltype, "f") && Objects.equals(rtype, "f"))
          yield (double) lobj + (double) robj;
        else if (Objects.equals(ltype, "i") && Objects.equals(rtype, "i"))
          yield (int) lobj + (int) robj;
        else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
      }
      case MINUS ->
          (lobj instanceof Double || robj instanceof Double)
              ? (double) lobj - (double) robj
              : (int) lobj - (int) robj;
      case EQUAL_EQUAL -> (boolean) lobj == (boolean) robj;
      case LESS -> {
        if (Objects.equals(ltype, "f") || Objects.equals(rtype, "f"))
          yield (double) lobj < (double) robj;
        else if (Objects.equals(ltype, "i") && Objects.equals(rtype, "i"))
          yield (int) lobj < (int) robj;
        else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
      }
      case GREATER -> {
        if (Objects.equals(ltype, "f") || Objects.equals(rtype, "f"))
          yield (double) lobj > (double) robj;
        else if (Objects.equals(ltype, "i") && Objects.equals(rtype, "i"))
          yield (int) lobj > (int) robj;
        else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
      }
      case LESS_EQUAL -> {
        if (Objects.equals(ltype, "f") || Objects.equals(rtype, "f"))
          yield (double) lobj <= (double) robj;
        else if (Objects.equals(ltype, "i") && Objects.equals(rtype, "i"))
          yield (int) lobj <= (int) robj;
        else throw new RunTimeEvalError(expr.op, "Operation not valid for these operands.");
      }
      case GREATER_EQUAL -> {
        if (Objects.equals(ltype, "f") || Objects.equals(rtype, "f"))
          yield (double) lobj >= (double) robj;
        else if (Objects.equals(ltype, "i") && Objects.equals(rtype, "i"))
          yield (int) lobj >= (int) robj;
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
    Object obj = evaluate(expr);
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

  void interpret(Expr expr) {
    try {
      Object obj = evaluate(expr);
      System.out.println(makeString(obj));
    } catch (RunTimeEvalError err) {
      Jlox.runTimeError(err);
    }
  }

  private String makeString(Object obj) {
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

  static record PairTypes(String ltype, String rtype) {}
}
