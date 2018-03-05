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

import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans and builds all classes in current classloader.
 */
public class ClassScanner {

    public List<Class<?>> getClassesForPackage(String packageName) throws ClassNotFoundException {
        final List<Class<?>> classes = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            ClassPath classPath = ClassPath.from(classLoader);

            // Some anonymous classes don't return true when calling isAnonymousClass(), but they
            // always seem to be nested anonymous classes like com.android.settings.Foo$1$2. In
            // general we don't want any anonymous classes so we just filter these out by searching
            // for $[0-9] in the name.
            Pattern anonymousClassPattern = Pattern.compile(".*\\$\\d+.*");
            Matcher anonymousClassMatcher = anonymousClassPattern.matcher("");

            for (ClassPath.ClassInfo info : classPath.getAllClasses()) {
                if (info.getPackageName().startsWith(packageName)) {
                    try {
                        Class clazz = classLoader.loadClass(info.getName());
                        if (clazz.isAnonymousClass() || anonymousClassMatcher.reset(
                                clazz.getName()).matches()) {
                            continue;
                        }
                        classes.add(clazz);
                    } catch (NoClassDefFoundError e) {
                        // do nothing. this class hasn't been found by the
                        // loader, and we don't care.
                    }
                }
            }
        } catch (final IOException e) {
            throw new ClassNotFoundException("Error when parsing " + packageName, e);
        }
        return classes;
    }
}
