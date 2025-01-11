package com.Jlox.tests;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.Jlox.LoxScanner;
import com.Jlox.Token;
import com.Jlox.Jlox;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class ScannerTests {
    private final PrintStream originalStdOut = System.out;
    private OutputStream consoleContent = new ByteArrayOutputStream();

    private final String resourcesDir = System.getProperty("resource.dir");

    @Test
    public void SimpleTest() throws IOException{
        Path inFilePath = Paths.get(resourcesDir, "kk.lox");
        System.setOut(new PrintStream(consoleContent));
        List<Token> tokens = (new LoxScanner(Files.readString(inFilePath))).scanTokens();
        for (Token token: tokens) {
            System.out.println(token);
        }
        String actual = consoleContent.toString();
        String expected = "LEFT_PAREN ( 0\n" +
                "RIGHT_PAREN ) 0\n" +
                "VAR var 0\n" +
                "IDENTIFIER kxk 0\n" +
                "IDENTIFIER varx 0\n" +
                "INTEGER 23 1\n" +
                "FLOAT 32. 1\n" +
                "LESS_EQUAL <= 2\n" +
                "LEFT_PAREN ( 3\n" +
                "INTEGER 4 3\n" +
                "PLUS + 3\n" +
                "INTEGER 3 3\n" +
                "RIGHT_PAREN ) 3\n" +
                "MINUS - 3\n" +
                "INTEGER 1 3\n" +
                "FLOAT 92.e9 4\n" +
                "ELSE else 4\n" +
                "STRING in2x 4\n" +
                "STRING fasf 5\n" +
                "STRING loo\\noong 6\n" +
                "EOF \\0 8\n";
        assertEquals(actual, expected);
    }

    @Test
    public void SimpleTest2() {
        Jlox.run("1. <= (1.+1.)");
        String actualOutput = consoleContent.toString().trim();
        boolean actual = Boolean.parseBoolean(actualOutput);
        assertEquals(actual, true);
    }

}
