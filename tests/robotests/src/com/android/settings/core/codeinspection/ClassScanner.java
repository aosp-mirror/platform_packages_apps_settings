/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.core.codeinspection;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans and builds all classes in current classloader.
 */
public class ClassScanner {

    private static final String CLASS_SUFFIX = ".class";

    public List<Class<?>> getClassesForPackage(String packageName)
            throws ClassNotFoundException {
        final List<Class<?>> classes = new ArrayList<>();

        try {
            final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
                    .getResources(packageName.replace('.', '/'));
            if (!resources.hasMoreElements()) {
                return classes;
            }
            URL url = resources.nextElement();
            while (url != null) {
                final URLConnection connection = url.openConnection();

                if (connection instanceof JarURLConnection) {
                    loadClassFromJar((JarURLConnection) connection, packageName,
                            classes);
                } else {
                    loadClassFromDirectory(new File(URLDecoder.decode(url.getPath(), "UTF-8")),
                            packageName, classes);
                }
                if (resources.hasMoreElements()) {
                    url = resources.nextElement();
                } else {
                    break;
                }
            }
        } catch (final IOException e) {
            throw new ClassNotFoundException("Error when parsing " + packageName, e);
        }
        return classes;
    }

    private void loadClassFromDirectory(File directory, String packageName, List<Class<?>> classes)
            throws ClassNotFoundException {
        if (directory.exists() && directory.isDirectory()) {
            final String[] files = directory.list();

            for (final String file : files) {
                if (file.endsWith(CLASS_SUFFIX)) {
                    try {
                        classes.add(Class.forName(
                                packageName + '.' + file.substring(0, file.length() - 6),
                                false /* init */,
                                Thread.currentThread().getContextClassLoader()));
                    } catch (NoClassDefFoundError e) {
                        // do nothing. this class hasn't been found by the
                        // loader, and we don't care.
                    }
                } else {
                    final File tmpDirectory = new File(directory, file);
                    if (tmpDirectory.isDirectory()) {
                        loadClassFromDirectory(tmpDirectory, packageName + "." + file, classes);
                    }
                }
            }
        }
    }

    private void loadClassFromJar(JarURLConnection connection, String packageName,
            List<Class<?>> classes) throws ClassNotFoundException, IOException {
        final JarFile jarFile = connection.getJarFile();
        final Enumeration<JarEntry> entries = jarFile.entries();
        String name;
        if (!entries.hasMoreElements()) {
            return;
        }
        JarEntry jarEntry = entries.nextElement();
        while (jarEntry != null) {
            name = jarEntry.getName();

            if (name.contains(CLASS_SUFFIX)) {
                name = name.substring(0, name.length() - CLASS_SUFFIX.length()).replace('/', '.');

                if (name.startsWith(packageName)) {
                    try {
                        classes.add(Class.forName(name,
                                false /* init */,
                                Thread.currentThread().getContextClassLoader()));
                    } catch (NoClassDefFoundError e) {
                        // do nothing. this class hasn't been found by the
                        // loader, and we don't care.
                    }
                }
            }
            if (entries.hasMoreElements()) {
                jarEntry = entries.nextElement();
            } else {
                break;
            }
        }
    }
}
