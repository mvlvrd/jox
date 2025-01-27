package com.Jlox.tests;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class Tests {
    protected static final PrintStream originalStdOut = System.out;
    protected static final PrintStream originalStdErr = System.err;

    protected OutputStream outContent;
    protected OutputStream errContent;

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
}
