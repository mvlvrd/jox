package com.Jlox.tests;

import org.testng.annotations.AfterMethod;

import java.io.File;
import java.io.PrintStream;

public class Tests {
    protected static final PrintStream originalStdOut = System.out;
    protected static final PrintStream originalStdErr = System.err;
    protected static final String resourcesDirName = System.getProperty("resource.dir");
    protected static final File resourcesDir = new File(resourcesDirName);

    @AfterMethod
    protected void resetStd() {
        System.setOut(originalStdOut);
        System.setErr(originalStdErr);
    }
}
