package org.lalber.tools.checkstyle.reflect;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * A parent-last classloader. Workaround for Java default behavior parent-first.
 */
public class ParentLastURLClassLoader extends URLClassLoader {

    private FindClassClassLoader parent;

    public ParentLastURLClassLoader(List<URL> classpath) {
        super(classpath.toArray(new URL[0]), null);
        parent = new FindClassClassLoader(getSystemClassLoader());
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            // first we try to find a class inside the child classloader
            return this.findClass(name);
        }
        catch (ClassNotFoundException e) {
            // didn't find it, try the parent
            return parent.loadClass(name);
        }
    }

    /**
     * Workaround for calling findClass on a Classloader
     */
    private static class FindClassClassLoader extends ClassLoader {
        public FindClassClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }
    }
}
