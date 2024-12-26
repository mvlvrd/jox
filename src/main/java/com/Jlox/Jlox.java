package com.Jlox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Jlox {
    private static boolean hadError = false;

    public static void run(String str) {
        LoxScanner scanner = new LoxScanner(str);
        List<Token> tokens = scanner.scanTokens();
        System.out.println(str);
        for (Token token: tokens)
            System.out.println(token);
    }

    public static void runFile(String filePath) throws IOException {
        run(Files.readString(Path.of(filePath)));
        if (hadError) System.exit(65);
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
        System.err.println("Error at line: " + line + errorMsg);
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Usage: Jlox [Script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }
}
