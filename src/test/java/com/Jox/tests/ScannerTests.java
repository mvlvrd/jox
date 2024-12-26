package com.Jox.tests;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import com.Jlox.Jlox;

import java.io.IOException;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class ScannerTests {
    private final PrintStream originalStdOut = System.out;
    private ByteArrayOutputStream consoleContent = new ByteArrayOutputStream();

    @Test
    public void SimpleTest() throws IOException{
        String inFilePath = "kk.lox";
        Jlox.runFile(inFilePath);
        String expected = "LEFT_PAREN null 0\n" +
                "RIGHT_PAREN null 0\n" +
                "VAR null 0\n" +
                "IDENTIFIER kxk 0\n" +
                "IDENTIFIER varx 1\n" +
                "INTEGER 23 1\n" +
                "FLOAT 32. 2\n" +
                "FLOAT 92.e9 2\n" +
                "ELSE null 2\n" +
                "STRING in2x 2\n" +
                "STRING fasf 3\n" +
                "EOF null 3\n";
        assertEquals(consoleContent.toString(), expected);
    }
}
