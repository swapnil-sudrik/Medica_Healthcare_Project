package com.fspl.medica_healthcare.utils;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LogDirectoryCreator {
    public static void createLogFolder() {
        String logDir = "logs/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MMMM"));
        File directory = new File(logDir);
        if (!directory.exists()) {
            directory.mkdirs(); // Create folder if it doesn't exist
        }
        System.setProperty("log.dir", directory.getAbsolutePath());
        reloadLog4jConfiguration();

    }
    private static void reloadLog4jConfiguration() {
        try {
            // Load from classpath
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader != null) {
                PropertyConfigurator.configure(classLoader.getResourceAsStream("log4j.properties"));
            } else {
                System.err.println("ERROR: Could not load Log4j configuration from classpath.");
            }
        } catch (Exception e) {
            System.err.println("Error reloading Log4j configuration: " + e.getMessage());
        }
    }
}

