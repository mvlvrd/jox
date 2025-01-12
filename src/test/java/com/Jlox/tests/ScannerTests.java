package com.Jlox.tests;

import static org.testng.Assert.assertEquals;

import com.Jlox.Jlox;
import com.Jlox.LoxScanner;
import com.Jlox.Token;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ScannerTests {
  private static final PrintStream originalStdOut = System.out;
  private final String resourcesDir = System.getProperty("resource.dir");

  @AfterMethod
  private void resetStdOut() {
    System.setOut(originalStdOut);
  }

  @Test
  public void SimpleTest() throws IOException {
    Path inFilePath = Paths.get(resourcesDir, "kk.lox");
    OutputStream consoleContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(consoleContent));
    List<Token> tokens = (new LoxScanner(Files.readString(inFilePath))).scanTokens();
    for (Token token : tokens) {
      System.out.println(token);
    }
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
  public void SimpleTest2(String input, String expectedOutput) {
    OutputStream consoleContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(consoleContent));
    Jlox.run(input);
    String actualOutput = consoleContent.toString().trim();
    assertEquals(actualOutput, expectedOutput);
  }
}
