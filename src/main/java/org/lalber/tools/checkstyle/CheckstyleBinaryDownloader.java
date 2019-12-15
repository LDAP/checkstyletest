package org.lalber.tools.checkstyle;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fusesource.jansi.Ansi.ansi;

public class CheckstyleBinaryDownloader {

    private static ResourceBundle res = Resource.getInstance().getBundle();
    private File csBinaries;
    private boolean print;

    public CheckstyleBinaryDownloader(String folder, boolean print) {
        this.csBinaries = new File(folder);
        csBinaries.mkdirs();
        this.print = print;
    }

    /**
     * Returns a Map containing all Available Versions of Checkstyle on Maven.
     * @return a sorted map with pairs (version, jarfile)
     */
    public SortedMap<String, File> download() {
        Set<String> versionsToCheck = getAvailableVersions();

        SortedMap<String, File> versionMap = new TreeMap<>(new VersionStringComparator());

        System.out.print(ansi().saveCursorPosition());

        int i = 1;
        for (String ver : versionsToCheck) {
            String filename = "checkstyle-" + ver + ".jar";
            File f = new File(csBinaries, filename);
            if (!f.exists()) {
                if (print)
                    System.out.print(ansi()
                                    .eraseLine()
                                    .restoreCursorPosition()
                                    .a(MessageFormat.format(res.getString("downloadprogmsg"),
                                            i++, versionsToCheck.size(), ver)));
                try {
                    FileUtils.copyURLToFile(new URL(res.getString("mavenCheckstyleURL") + ver + "/" + filename), f);
                    versionMap.put(ver, f);
                } catch (IOException e) {
                    System.err.print("Failed: ");
                    System.err.println(e.getMessage());
                    System.err.println("Skipping...");
                }
            } else {
                versionMap.put(ver, f);
            }
        }

        if (print)
            System.out.println();

        return versionMap;
    }

    private Set<String> getAvailableVersions() {
        Set<String> available = new TreeSet<>();
        try (BufferedInputStream in = new BufferedInputStream(new URL(res.getString("mavenCheckstyleMeta")).openStream())) {
            Scanner s = new Scanner(in);
            Pattern versionPattern = Pattern.compile(".*<version>(\\d.\\d{1,2})</version>.*");
            while(s.hasNext()) {
                Matcher m = versionPattern.matcher(s.nextLine());
                if (m.matches())
                    available.add(m.group(1));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return available;
    }

}
