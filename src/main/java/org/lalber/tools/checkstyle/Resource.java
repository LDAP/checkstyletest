package org.lalber.tools.checkstyle;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Resource {
    private static final Resource instance = new Resource();
    private ResourceBundle bundle = PropertyResourceBundle.getBundle("res");

    public ResourceBundle getBundle() {
        return bundle;
    }

    public static Resource getInstance() {
        return instance;
    }
}
