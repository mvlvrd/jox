package com.Jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Jlox {
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();
    private static final AstPrinter astPrinter = new AstPrinter();

    static int MAX_ARITY = 255;
    static boolean DEBUG_MODE = false;

    private static OperationMode opMode = OperationMode.NONE;

    enum OperationMode {
        NONE,
        SCRIPT,
        CONSOLE,
        COMMAND
    }

    public static void run(String str) {
        hadError = false;
        hadRuntimeError = false;
        LoxScanner scanner = new LoxScanner(str);
        List<Token> tokens = scanner.scanTokens();
        if (DEBUG_MODE) tokens.forEach(System.out::println);
        Parser parser = new Parser(tokens);
        // TODO: This should be more robust.
        if (isListOfStmts(tokens)) {
            List<Stmt> stmts = parser.parse();
            if (DEBUG_MODE) System.out.println(stmts.stream().map(astPrinter::print));
            if (hadError) return;
            Resolver resolver = new Resolver(interpreter);
            resolver.resolve(stmts);
            if (hadError) return;
            interpreter.interpret(stmts);
        } else {
            Expr expr = parser.parseSingleExpr();
            if (DEBUG_MODE) System.out.println(astPrinter.print(expr));
            if (hadError) return;
            interpreter.interpret(expr);
        }
    }

    public static void runFile(String filePath) throws IOException {
        runFile(Path.of(filePath));
    }

    public static void runFile(Path filePath) throws IOException {
        run(Files.readString(filePath));
        if (hadError) throw new LoxError(65);
        if (hadRuntimeError) throw new LoxError(70);
    }

    public static void runPrompt() throws IOException {
        try (InputStreamReader input = new InputStreamReader(System.in);
                BufferedReader reader = new BufferedReader(input)) {
            while (true) {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null) break;
                run(line);
                hadError = false;
            }
        }
    }

    static void error(int line, String errorMsg) {
        hadError = true;
        System.err.println("Error at line: " + line + " " + errorMsg);
    }

    static void error(Token token, String errorMsg) {
        hadError = true;
        System.err.println("Error at line: " + token.line + ": " + token.lexeme + ". " + errorMsg);
    }

    static void runTimeError(RunTimeEvalError err) {
        hadRuntimeError = true;
        System.err.println(err.getMessage() + "\n[Line:" + err.token.line + "]");
    }

    private static void checkArgs(String[] args) throws IOException {
        if (args.length > 2) {
            System.err.println(UsageMessg);
            throw new LoxError(64);
        } else if (args.length == 2) {
            if (args[0].equals("-c")) {
                opMode = OperationMode.COMMAND;
                run(args[1]);
            } else {
                System.err.println(UsageMessg);
                throw new LoxError(64);
            }
        } else if (args.length == 1) {
            opMode = OperationMode.SCRIPT;
            runFile(args[0]);
        } else {
            opMode = OperationMode.CONSOLE;
            runPrompt();
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            checkArgs(args);
        } catch (LoxError err) {
            System.exit(err.exitCode);
        }
    }

    private static boolean isListOfStmts(List<Token> tokens) {
        TokenType tokenType = tokens.get(tokens.size() - 2).type;
        return (tokenType == TokenType.SEMICOLON || tokenType == TokenType.RIGHT_BRACE);
    }

    private static final String UsageMessg = "Usage: Jlox [Script|-c command]";
}
