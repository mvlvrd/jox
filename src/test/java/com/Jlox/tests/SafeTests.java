package com.Jlox.tests;

import static org.testng.Assert.assertEquals;

import com.Jlox.Jlox;
import com.Jlox.LoxError;
import com.Jlox.LoxScanner;
import com.Jlox.Token;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class SafeTests extends Tests {

    protected static final String resourcesDirName = System.getProperty("resource.dir");
    protected static final File resourcesDir = new File(resourcesDirName);

    //private static final Pattern filesPattern = Pattern.compile("^Res\\d+.lox$");
    private static final Pattern filesPattern = Pattern.compile("^_Res\\d+.lox$");

    protected static String[] MakeFileTuple(File file) {
        String inFile = file.getName();
        String outFile = inFile.replace(".lox", ".txt");
        String errFile = inFile.replace(".lox", ".err");
        return new String[] {
            inFile,
            (new File(resourcesDirName, outFile)).exists() ? outFile : null,
            (new File(resourcesDirName, errFile)).exists() ? errFile : null
        };
    }

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

    @DataProvider(name = "StringData")
    String[][] StringData() {
        return new String[][] {{"1. <= (1.+1.)", "true"}, {"\"3\"==\"2\"", "false"}, {"3+2", "5"}};
    }

    @Test(dataProvider = "StringData")
    public void StringTest(String input, String expectedOutput) {
        Jlox.run(input);
        String actualOutput = outContent.toString().trim();
        assertEquals(actualOutput, expectedOutput);
    }

    @DataProvider(name = "FileData")
    Iterator<String[]> FileData() {
        return Arrays.stream(resourcesDir.listFiles())
                .filter(file -> file.isFile() && filesPattern.matcher(file.getName()).matches())
                .map(SafeTests::MakeFileTuple).iterator();
    }

    @Test(dataProvider = "FileData")
    public void FileTest(String inFileName, String outFileName, String errFileName)
            throws IOException {
        // TODO: Check the exit code.
        try {
            Jlox.runFile(Paths.get(resourcesDirName, inFileName));
        } catch (LoxError err) {
            if (errFileName == null) throw err;
        }

        assertEquals(outContent.toString(), readFile(outFileName));
        assertEquals(errContent.toString(), readFile(errFileName));
    }

    private static String readFile(String fileName) throws IOException {
        return (fileName != null) ? Files.readString(Paths.get(resourcesDirName, fileName)) : "";
    }
}
