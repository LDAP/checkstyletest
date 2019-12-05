package org.lalber.tools.checkstyle;

import java.util.Comparator;
import java.util.StringTokenizer;

public class VersionStringComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {

        StringTokenizer t1 = new StringTokenizer(o1, ".");
        StringTokenizer t2 = new StringTokenizer(o2, ".");

        while (t1.hasMoreTokens() && t2.hasMoreTokens()) {
            int compare = compareNumberStrings(t1.nextToken(), t2.nextToken());
            if (compare != 0) return compare;
        }
        if (t1.hasMoreTokens()) return 1;
        if (t2.hasMoreTokens()) return -1;
        return 0;
    }


    private static int compareNumberStrings(String o1, String o2) {
        return Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2));
    }
}
