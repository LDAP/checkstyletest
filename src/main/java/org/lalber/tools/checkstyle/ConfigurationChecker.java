package org.lalber.tools.checkstyle;

import org.lalber.tools.checkstyle.reflect.ParentLastURLClassLoader;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Checks Checkstyle XML-Files.
 */
public class ConfigurationChecker {

    private static ResourceBundle res = Resource.getInstance().getBundle();

    private File jarFile;
    private String versionString;

    public ConfigurationChecker(String versionString, File jarFile) {
        this.versionString = versionString;
        this.jarFile = jarFile;
    }

    public Result check(File xmlFile) {
        try {
            ClassLoader cl = new ParentLastURLClassLoader(List.of(jarFile.toURI().toURL()));
            Class configLoader = cl.loadClass("com.puppycrawl.tools.checkstyle.ConfigurationLoader");
            Class propertyRes = cl.loadClass("com.puppycrawl.tools.checkstyle.PropertyResolver");
            Class propertyExp = cl.loadClass("com.puppycrawl.tools.checkstyle.PropertiesExpander");
            Class checkerCls = cl.loadClass("com.puppycrawl.tools.checkstyle.Checker");
            Class configCls = cl.loadClass("com.puppycrawl.tools.checkstyle.api.Configuration");

            InputSource is = new InputSource(new FileInputStream(xmlFile));
            Object pr = propertyExp.getDeclaredConstructor(Properties.class).newInstance(System.getProperties());

            Object config = null;
            // Compensate change in checkstyle api
            if (new VersionStringComparator().compare(versionString, "8.2") >= 0) {
                Class imo = cl.loadClass("com.puppycrawl.tools.checkstyle.ConfigurationLoader$IgnoredModulesOptions");
                Method loadConfig = loadConfig = configLoader.getDeclaredMethod("loadConfiguration",
                        InputSource.class, propertyRes, imo);
                config = loadConfig.invoke(null, is, pr, imo.getEnumConstants()[0]);
            } else {
                Method loadConfig = configLoader.getDeclaredMethod("loadConfiguration",
                        InputSource.class, propertyRes, boolean.class);
                config = loadConfig.invoke(null, is, pr, true);
            }

            Object checker = checkerCls.getDeclaredConstructor().newInstance();
            checkerCls.getDeclaredMethod("setModuleClassLoader", ClassLoader.class).invoke(checker, checkerCls.getClassLoader());
            Method m = checkerCls.getMethod("configure", configCls);
            m.invoke(checker, config);

            return new Result(Type.SUCCESS, null);

        } catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException | FileNotFoundException | IllegalAccessException | InstantiationException e) {
            return new Result(Type.NOT_SUPPORTED, e.getMessage());
        } catch (InvocationTargetException e) {
            return new Result(Type.FAIL, null);
        }
    }

    public class Result {
        private String additionalInfo;
        private Type type;

        public Result(Type type, String additionalInfo) {
            this.additionalInfo = additionalInfo;
            this.type = type;
        }

        public String getAdditionalInfo() {return additionalInfo;}
        public Type getType() {return type;}
    }

    public enum Type {
        SUCCESS(ansi().fgBrightGreen().a(res.getString("successmsg")).reset().toString(), false),
        FAIL(ansi().bold().fgBrightRed().a(res.getString("failmsg")).reset().toString(), false),
        NOT_SUPPORTED(res.getString("notsupportedmsg"), true);

        private String stringRep;
        private boolean hasAdditionalInfo;

        Type(String stringRep, boolean hasAdditionalInfo) {
            this.stringRep = stringRep;
            this.hasAdditionalInfo = hasAdditionalInfo;
        }

        public String getStringRep() {
            return stringRep;
        }

        public boolean hasAdditionalInfo() {
            return hasAdditionalInfo;
        }
    };
}
