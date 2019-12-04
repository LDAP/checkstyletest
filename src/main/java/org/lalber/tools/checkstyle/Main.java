package org.lalber.tools.checkstyle;

import org.apache.commons.io.FileUtils;
import org.xml.sax.InputSource;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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

    private static String mavenCheckstyleMeta = "https://repo1.maven.org/maven2/com/puppycrawl/tools/checkstyle/maven-metadata.xml";
    private static String mavenCheckstyleURL = "https://repo1.maven.org/maven2/com/puppycrawl/tools/checkstyle/";

    private static ResourceBundle res = PropertyResourceBundle.getBundle("res", new ResourceBundle.Control() {
        @Override
        public List<Locale> getCandidateLocales(String s, Locale locale) {
            return Arrays.asList(Locale.getDefault(), Locale.ENGLISH);
        }
    });

    /**
     * Main entry point to Java application.
     * @param args the name of the config file to check.
     */
    public static void main(String args[]) throws InstantiationException {
        System.out.println(MessageFormat.format(res.getString("welcomemgs"), Main.class.getPackage().getImplementationVersion()));

        if (args.length != 1 || !new File(args[0]).exists()) {
            System.out.println("Provide Checkstyle XML-File as argument!");
            System.exit(1);
        }
        File xml = new File(args[0]);

        System.out.println(res.getString("downloadmsg"));

        File csBinarys = new File(".cs_test/");
        csBinarys.mkdirs();

        Set<String> versionsToCheck = new TreeSet<>();
        try (BufferedInputStream in = new BufferedInputStream(new URL(mavenCheckstyleMeta).openStream())) {
            Scanner s = new Scanner(in);

            Pattern versionPattern = Pattern.compile(".*<version>(\\d.\\d{1,2})</version>.*");
            while(s.hasNext()) {
                Matcher m = versionPattern.matcher(s.nextLine());
                if (m.matches())
                    versionsToCheck.add(m.group(1));
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        int i = 1;
        for (String ver : versionsToCheck) {
            String filename = "checkstyle-" + ver + ".jar";
            if (!new File(csBinarys, filename).exists()) {
                System.out.println(MessageFormat.format(res.getString("downloadprogmsg"), i++, versionsToCheck.size(), ver));
                try {
                    FileUtils.copyURLToFile(new URL(mavenCheckstyleURL + ver + "/" + filename), new File(csBinarys, filename));
                } catch (IOException e) {
                    System.err.print("Failed: ");
                    System.err.println(e.getMessage());
                    System.err.println("Skipping...");
                }
            }
        }

        File[] files = csBinarys.listFiles();
        sort(files, (o1, o2) -> compareVersionStrings(
                o1.getName().substring(o1.getName().indexOf("-") + 1, o1.getName().lastIndexOf(".")),
                o2.getName().substring(o2.getName().indexOf("-") + 1, o2.getName().lastIndexOf("."))));
        for (File bin : files) {
            try {
                String version = bin.getName().substring(bin.getName().indexOf("-") + 1, bin.getName().lastIndexOf("."));
                System.out.print(bin.getName().substring(0, bin.getName().lastIndexOf(".")));

                ClassLoader cl = new ParentLastURLClassLoader(List.of(bin.toURI().toURL()));
                Class configLoader = cl.loadClass("com.puppycrawl.tools.checkstyle.ConfigurationLoader");
                Class propertyRes = cl.loadClass("com.puppycrawl.tools.checkstyle.PropertyResolver");
                Class imo = null;
                if (compareVersionStrings(version, "8.2") >= 0)
                    imo = cl.loadClass("com.puppycrawl.tools.checkstyle.ConfigurationLoader$IgnoredModulesOptions");
                Class propertyExp = cl.loadClass("com.puppycrawl.tools.checkstyle.PropertiesExpander");
                Class checkerCls = cl.loadClass("com.puppycrawl.tools.checkstyle.Checker");
                Class configCls = cl.loadClass("com.puppycrawl.tools.checkstyle.api.Configuration");

                Method loadConfig = null;
                if (compareVersionStrings(version, "8.2") >= 0)
                    loadConfig = configLoader.getDeclaredMethod("loadConfiguration",
                        InputSource.class, propertyRes, imo);
                else
                    loadConfig = configLoader.getDeclaredMethod("loadConfiguration",
                        InputSource.class, propertyRes, boolean.class);

                InputSource is = new InputSource(new FileInputStream(xml));
                //PropertyResolver pr = new PropertiesExpander(System.getProperties());
                Object pr = propertyExp.getDeclaredConstructor(Properties.class).newInstance(System.getProperties());

                Object config = null;
                if (compareVersionStrings(version, "8.2") >= 0)
                    config = loadConfig.invoke(null, is, pr, imo.getEnumConstants()[0]);
                else
                    config = loadConfig.invoke(null, is, pr, true);

                Object checker = checkerCls.getDeclaredConstructor().newInstance();

                checkerCls.getDeclaredMethod("setModuleClassLoader", ClassLoader.class).invoke(checker, checkerCls.getClassLoader());

                Method m = checkerCls.getMethod("configure", configCls);
                m.invoke(checker, config);

                System.out.println(" OK!");

            } catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException | FileNotFoundException | IllegalAccessException e) {
                //e.printStackTrace();
                //System.out.println(e.getMessage());
                System.out.println(" Not Supported! Skipping...");
            } catch (InvocationTargetException e) {
                System.out.println(" failed!");
            }
        }

    }


    private static int compareVersionStrings(String o1, String o2) {
        String[] o1Split = o1.split("\\.");
        String[] o2Split = o2.split("\\.");

        int a = Integer.compare(Integer.parseInt(o1Split[0]), Integer.parseInt(o2Split[0]));
        return a != 0 ? a : Integer.compare(Integer.parseInt(o1Split[1]), Integer.parseInt(o2Split[1]));
    }

}
