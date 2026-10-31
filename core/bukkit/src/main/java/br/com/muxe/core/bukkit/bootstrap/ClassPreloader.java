package br.com.muxe.core.bukkit.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class ClassPreloader {

    private static final String CLASS_SUFFIX = ".class";

    private ClassPreloader() {
    }

    public static int preload(ClassLoader loader, File jar, String packagePrefix) throws IOException {
        try (JarFile file = new JarFile(jar)) {
            return preload(loader, file.stream().map(JarEntry::getName), packagePrefix);
        }
    }

    public static int preload(ClassLoader loader, Stream<String> entries, String packagePrefix) {
        String prefix = packagePrefix.replace('.', '/') + "/";
        int loaded = 0;
        Iterator<String> names = entries.filter(entry -> entry.startsWith(prefix) && entry.endsWith(CLASS_SUFFIX)).iterator();
        while (names.hasNext()) {
            String entry = names.next();
            String name = entry.substring(0, entry.length() - CLASS_SUFFIX.length()).replace('/', '.');
            try {
                Class.forName(name, false, loader);
                loaded++;
            } catch (ClassNotFoundException | LinkageError _) {
                continue;
            }
        }
        return loaded;
    }
}
