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
    private static Phase phase = Phase.NONE;

    enum OperationMode {
        NONE,
        SCRIPT,
        CONSOLE,
        COMMAND
    }

    enum Phase {
        NONE,
        PARSER,
        RESOLVER,
        INTERPRETER
    }

    public static void run(String str) {
        hadError = false;
        hadRuntimeError = false;
        LoxScanner scanner = new LoxScanner(str);
        List<Token> tokens = scanner.scanTokens();
        if (DEBUG_MODE) tokens.forEach(System.out::println);
        Parser parser = new Parser(tokens);
        Resolver resolver = new Resolver(interpreter);
        // TODO: This should be more robust.
        if (isListOfStmts(tokens)) {
            phase = Phase.PARSER;
            List<Stmt> stmts = parser.parse();
            if (DEBUG_MODE) System.out.println(stmts.stream().map(astPrinter::print));
            if (hadError) return;
            phase = Phase.RESOLVER;
            resolver.resolve(stmts);
            if (hadError) return;
            phase = Phase.INTERPRETER;
            interpreter.interpret(stmts);
        } else if (!(tokens.isEmpty()
                || (tokens.size() == 1 && tokens.getFirst().type == TokenType.EOF))) {
            phase = Phase.PARSER;
            Expr expr = parser.parseSingleExpr();
            if (DEBUG_MODE) System.out.println(astPrinter.print(expr));
            if (hadError) return;
            phase = Phase.RESOLVER;
            resolver.resolve(expr);
            if (hadError) return;
            phase = Phase.INTERPRETER;
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

    static void report(int line, String where, String message) {
        if (DEBUG_MODE) System.err.println("Current phase: " + phase);
        System.err.println("[line " + (line + 1) + "] Error" + where + ": " + message);
    }

    static void error(int line, String errorMsg) {
        hadError = true;
        report(line, "", errorMsg);
    }

    static void error(Token token, String errorMsg) {
        hadError = true;
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", errorMsg);
        } else {
            report(token.line, " at '" + token.lexeme + "'", errorMsg);
        }
    }

    static void runTimeError(RunTimeEvalError err) {
        hadRuntimeError = true;
        // TODO: Add line after passing tests.
        System.err.println(err.getMessage()); // + "\n[Line:" + err.token.line + "]");
    }

    private static void ArgErr() {
        System.err.println(UsageMessg);
        throw new LoxError(64);
    }

    private static void checkArgs(String[] args) throws IOException {
        if (args.length == 0) {
            opMode = OperationMode.CONSOLE;
            runPrompt();
        } else if (args[0].equals("-c")) {
            opMode = OperationMode.COMMAND;
            if (args.length < 2) ArgErr();
            String command = args[1];
            if (args.length == 3 && args[2].equals("--debug")) DEBUG_MODE = true;
            else if (args.length > 2) ArgErr();
            run(command);
        } else {
            opMode = OperationMode.SCRIPT;
            if (args.length == 2 && args[1].equals("--debug")) DEBUG_MODE = true;
            else if (args.length > 2) ArgErr();
            runFile(args[0]);
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
        if (tokens.size() <= 1) return false;
        TokenType tokenType = tokens.get(tokens.size() - 2).type;
        return (tokenType == TokenType.SEMICOLON || tokenType == TokenType.RIGHT_BRACE);
    }

    private static final String UsageMessg = "Usage: Jlox [Script|-c command]";
}
