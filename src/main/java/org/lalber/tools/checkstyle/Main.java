package org.lalber.tools.checkstyle;

import org.apache.commons.io.FileUtils;
import org.fusesource.jansi.AnsiConsole;
import org.lalber.tools.checkstyle.reflect.ParentLastURLClassLoader;
import org.xml.sax.InputSource;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.sort;

/**
 * Application to test checkstyle XML config files for compatibility.
 * Usage: java -jar JARFILE config.xml
 */
public class Main {

    private static ResourceBundle res = Resource.getInstance().getBundle();

    static {
        AnsiConsole.systemInstall();
    }

    /**
     * Main entry point to Java application.
     * @param args the name of the config file to check.
     */
    public static void main(String args[]) throws InstantiationException {
        System.out.println(MessageFormat.format(res.getString("welcomemgs"),
                Main.class.getPackage().getImplementationVersion()));

        File xml = getCheckstyleFile(args);

        System.out.println(res.getString("downloadmsg"));

        SortedMap<String, File> toCheck = new CheckstyleBinaryDownloader(".cs_test/", true).download();

        for (Map.Entry<String, File> checkstyle : toCheck.entrySet()) {
            System.out.print(MessageFormat.format(res.getString("checkmsg"), checkstyle.getKey()));
            ConfigurationChecker.Result r = new ConfigurationChecker(checkstyle.getKey(), checkstyle.getValue())
                    .check(xml);
            System.out.println(" " + r.getType().getStringRep());
        }


    }

    private static File getCheckstyleFile(String[] args) {
        if (args.length != 1 || !new File(args[0]).exists()) {
            System.err.println(res.getString("nofile"));
            System.exit(1);
        }
        return new File(args[0]);
    }

}
