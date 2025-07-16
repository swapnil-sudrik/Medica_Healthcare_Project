package com.fspl.medica_healthcare.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {

    public static String getStackTrace(Exception e) {
        //create a log folder
        LogDirectoryCreator.createLogFolder();
        // Create a StringWriter to capture the stack trace in memory.
        StringWriter sw = new StringWriter();

        // Create a PrintWriter that wraps around the StringWriter.
        // PrintWriter is used because e.printStackTrace() requires a PrintWriter or PrintStream.
        PrintWriter pw = new PrintWriter(sw);

        // Print the exception stack trace into the PrintWriter,
        // which writes the output into the underlying StringWriter.
        e.printStackTrace(pw);

        // Convert the StringWriter's content (stack trace) into a String and return it.
        return sw.toString();
    }
}

