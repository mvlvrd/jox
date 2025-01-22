package com.Jlox.tests;

import static org.testng.Assert.assertEquals;

import com.Jlox.Jlox;
import com.Jlox.LoxError;
import com.Jlox.LoxScanner;
import com.Jlox.Token;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class SafeTests extends Tests {

    @Test
    public void ScannerTest() throws IOException {
        Path inFilePath = Paths.get(resourcesDirName, "scanTest.lox");
        OutputStream consoleContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(consoleContent));
        List<Token> tokens = (new LoxScanner(Files.readString(inFilePath))).scanTokens();
        tokens.forEach(System.out::println);
        String actual = consoleContent.toString();
        String expected =
                """
                LEFT_PAREN ( 0
                RIGHT_PAREN ) 0
                VAR var 0
                IDENTIFIER kxk 0
                IDENTIFIER varx 0
                INTEGER 23 1
                FLOAT 32. 1
                LESS_EQUAL <= 2
                LEFT_PAREN ( 3
                INTEGER 4 3
                PLUS + 3
                INTEGER 3 3
                RIGHT_PAREN ) 3
                MINUS - 3
                INTEGER 1 3
                FLOAT 92.e9 4
                ELSE else 4
                STRING in2x 4
                STRING fasf 5
                STRING loo\\noong 6
                EOF \\0 8
                """;
        assertEquals(actual, expected);
    }

    @DataProvider(name = "Test1")
    String[][] Test1Data() {
        return new String[][] {{"1. <= (1.+1.)", "true"}, {"\"3\"==\"2\"", "false"}, {"3+2", "5"}};
    }

    @Test(dataProvider = "Test1")
    public void InterpreterTest1(String input, String expectedOutput) {
        OutputStream consoleContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(consoleContent));
        Jlox.run(input);
        String actualOutput = consoleContent.toString().trim();
        assertEquals(actualOutput, expectedOutput);
    }

    @DataProvider(name = "Test2")
    Iterator<String[]> Test2Data() {
        Pattern pattern = Pattern.compile("^Res\\d+.lox$");
        return Arrays.stream(resourcesDir.listFiles())
                .filter(file -> file.isFile() && pattern.matcher(file.getName()).matches())
                .map(SafeTests::MakeFileTuple)
                .iterator();
    }

    @Test(dataProvider = "Test2")
    public void FileTest2(String inFileName, String outFileName, String errFileName)
            throws IOException {
        OutputStream consoleContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(consoleContent));
        if (outFileName == null) return;

        Jlox.runFile(Paths.get(resourcesDirName, inFileName));
        String actual = consoleContent.toString();
        String expectedOutput = Files.readString(Paths.get(resourcesDirName, outFileName));
        assertEquals(actual, expectedOutput);
    }

    @Test(dataProvider = "Test2")
    public void JustErrTest2(String inFileName, String outFileName, String errFileName)
            throws IOException {
        OutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));
        if (errFileName == null) return;

        // TODO: Check the exit code.
        try {
            Jlox.runFile(Paths.get(resourcesDirName, inFileName));
        } catch (LoxError err) {
            if (errFileName == null) throw err;
        }

        String actualErr = errContent.toString();
        String expectedErr = Files.readString(Paths.get(resourcesDirName, errFileName));
        assertEquals(actualErr, expectedErr);
    }

    // TODO: Merge stdout and stderr tests in the same method.
    // @Test(dataProvider = "Test2")
    public void FullTest2(String inFileName, String outFileName, String errFileName)
            throws IOException {
        OutputStream consoleContent = new ByteArrayOutputStream();
        OutputStream errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(consoleContent));
        System.setErr(new PrintStream(errContent));

        // TODO: Check the exit code.
        try {
            Jlox.runFile(Paths.get(resourcesDirName, inFileName));
        } catch (LoxError err) {
            if (errFileName == null) throw err;
        }
        String actual = consoleContent.toString();
        String expectedOutput =
                (outFileName != null)
                        ? Files.readString(Paths.get(resourcesDirName, outFileName))
                        : "";
        assertEquals(actual, expectedOutput);

        String actualErr = errContent.toString();
        String expectedErr =
                (errFileName != null)
                        ? Files.readString(Paths.get(resourcesDirName, errFileName))
                        : "";
        assertEquals(actualErr, expectedErr);
    }

    private static String[] MakeFileTuple(File file) {
        String inFile = file.getName();
        String outFile = inFile.replace(".lox", ".txt");
        String errFile = inFile.replace(".lox", ".err");
        return new String[] {
            inFile,
            (new File(resourcesDirName, outFile)).exists() ? outFile : null,
            (new File(resourcesDirName, errFile)).exists() ? errFile : null
        };
    }
}
