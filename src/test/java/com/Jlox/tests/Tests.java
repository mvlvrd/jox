package com.Jlox.tests;

import static org.testng.Assert.assertEquals;

import com.Jlox.Jlox;
import com.Jlox.LoxError;
import com.Jlox.LoxScanner;
import com.Jlox.Token;

import org.testng.annotations.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tests {
    protected static final PrintStream originalStdOut = System.out;
    protected static final PrintStream originalStdErr = System.err;

    protected OutputStream outContent;
    protected OutputStream errContent;

    protected static final String resourcesDirName = System.getProperty("resource.dir");

    protected static final Path FileTests = Paths.get(resourcesDirName, "tests");
    private static final Path OfficialTestDirName = Paths.get(resourcesDirName, "officialTests");

    private static List<String> WhiteList = new ArrayList<>();

    @BeforeClass
    static void setUp() {
        try {
            WhiteList = Files.lines(Paths.get(resourcesDirName, "WhiteList.txt")).toList();
        } catch (IOException err) {
            throw new RuntimeException("Failed to read whitelist file.", err);
        }
    }

    private static final Pattern filesPattern = Pattern.compile("^Res\\d+.lox$");

    private static final Pattern assertPattern =
            Pattern.compile("// expect: (.*)$", Pattern.MULTILINE);
    private static final Pattern errPattern =
            Pattern.compile(
                    "// (expect runtime error: (.*)|(\\[line \\d+\\] Error.*))$",
                    Pattern.MULTILINE);
    private static final Pattern floatPattern =
            Pattern.compile("^// expect: \\*\\*FLOAT\\*\\*$", Pattern.MULTILINE);

    @BeforeMethod
    protected void setStd() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterMethod
    protected void resetStd() {
        System.setOut(originalStdOut);
        System.setErr(originalStdErr);
    }

    private static String[] MakeFileTuple(Path path) {
        String inFile = path.toString();
        String outFile = inFile.replace(".lox", ".txt");
        String errFile = inFile.replace(".lox", ".err");
        return new String[] {
            inFile,
            (new File(outFile)).exists() ? outFile : null,
            (new File(errFile)).exists() ? errFile : null
        };
    }

    @Test()
    public void ScannerTest() throws IOException {
        Path inFilePath = Paths.get(resourcesDirName, "tests/scanTest.lox");
        List<Token> tokens = (new LoxScanner(Files.readString(inFilePath))).scanTokens();
        tokens.forEach(System.out::println);
        String actual = outContent.toString();
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
    private String[][] StringData() {
        return new String[][] {
            {"1. <= (1.+1.)", "true"}, {"\"3\"==\"2\"", "false"}, {"3+2", "5"}, {"!(1==2)", "true"}
        };
    }

    @Test(dataProvider = "StringData")
    public void StringTest(String input, String expectedOutput) {
        Jlox.run(input);
        String actualOutput = outContent.toString().trim();
        assertEquals(actualOutput, expectedOutput);
    }

    @DataProvider(name = "FileData")
    Iterator<String[]> FileData() throws IOException {
        return Files.walk(FileTests)
                .filter(
                        path ->
                                Files.isRegularFile(path)
                                        && filesPattern
                                                .matcher(path.getFileName().toString())
                                                .matches())
                .map(Tests::MakeFileTuple)
                .iterator();
    }

    @Test(dataProvider = "FileData")
    public void FileTest(String inFileName, String outFileName, String errFileName)
            throws IOException {
        // TODO: Check the exit code.
        try {
            Jlox.runFile(inFileName);
        } catch (LoxError err) {
            // if (errFileName == null) throw err;
        }

        assertEquals(outContent.toString(), readFile(outFileName));
        assertEquals(errContent.toString(), readFile(errFileName));
    }

    @DataProvider(name = "OfficialData")
    public Iterator<String[]> OfficialData() throws IOException {
        return Files.walk(OfficialTestDirName)
                .filter(Files::isRegularFile)
                .map(path -> new String[] {path.toString()})
                .iterator();
    }

    @Test(dataProvider = "OfficialData")
    public void OfficialTest(String inFileName) throws IOException {
        String inText = readFile(inFileName);
        String expectedOut = MatchAssert(inText);
        String expectedErr = MatchErr(inText);

        // TODO: Check the exit code.
        try {
            Jlox.runFile(inFileName);
        } catch (LoxError err) {
            // if (!errTest.isEmpty()) throw err;
        } catch (VirtualMachineError ignored) {
        }

        String actualOut = outContent.toString();
        String actualErr = errContent.toString();
        expectedOut = sanitizeFloat(expectedOut, actualOut);

        try {
            assertEquals(actualOut, expectedOut);
            assertEquals(actualErr, expectedErr);
        } catch (AssertionError err) {
            if (IsWhiteListed(inFileName)) throw err;
        }
    }

    private static String MatchAssert(String text) {
        Matcher matcher = assertPattern.matcher(text);
        StringBuilder bldr = new StringBuilder();
        while (matcher.find()) {
            bldr.append(matcher.group(1)).append("\n");
        }
        return bldr.toString();
    }

    private static String MatchErr(String text) {
        Matcher matcher = errPattern.matcher(text);
        StringBuilder bldr = new StringBuilder();
        while (matcher.find()) {
            bldr.append(matcher.group(2) != null ? matcher.group(2) : matcher.group(3))
                    .append("\n");
        }
        return bldr.toString();
    }

    private static String readFile(String fileName) throws IOException {
        if (fileName == null) return "";
        Path path = Paths.get(fileName);
        return Files.isRegularFile(path) ? Files.readString(path) : "";
    }

    private static String sanitizeFloat(String expected, String actual) {
        return (floatPattern.matcher(expected).matches())
                ? actual.replaceAll("\\d+(.\\d*)]", "**FLOAT**")
                : actual;
    }

    private static boolean IsWhiteListed(String fileName) {
        for (String cand : WhiteList) {
            if (cand.endsWith(fileName)) return true;
        }
        return false;
    }
}
