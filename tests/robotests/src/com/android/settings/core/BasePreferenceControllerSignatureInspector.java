/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.core;

import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;

import com.android.settings.core.codeinspection.CodeInspector;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class BasePreferenceControllerSignatureInspector extends CodeInspector {

    private final List<String> grandfather;

    public BasePreferenceControllerSignatureInspector(List<Class<?>> classes) {
        super(classes);
        grandfather = new ArrayList<>();
        initializeGrandfatherList(grandfather,
                "grandfather_invalid_base_preference_controller_constructor");
    }

    @Override
    public void run() {
        StringBuilder badClasses = new StringBuilder();

        for (Class c : mClasses) {
            if (!isConcreteSettingsClass(c)) {
                // Not a Settings class, or is abstract, don't care.
                continue;
            }
            if (!BasePreferenceController.class.isAssignableFrom(c)) {
                // Not a BasePreferenceController, don't care.
                continue;
            }
            final String className = c.getName();
            if (grandfather.remove(className)) {
                continue;
            }
            final Constructor[] constructors = c.getDeclaredConstructors();
            if (constructors == null || constructors.length == 0) {
                badClasses.append(c.getName()).append(",");
            }

            boolean hasValidConstructor = false;
            for (Constructor constructor : constructors) {
                if (hasValidConstructorSignature(constructor)) {
                    hasValidConstructor = true;
                    break;
                }
            }
            if (!hasValidConstructor) {
                badClasses.append(className).append(",");
            }
        }

        assertWithMessage("All BasePreferenceController (and subclasses) constructor must either"
                + " only take Context, or (Context, String). No other types are allowed")
                .that(badClasses.toString())
                .isEmpty();

        assertWithMessage("Something in the grandfather list is no longer relevant. Please remove"
            + "it from packages/apps/Settings/tests/robotests/assets/"
            + "grandfather_invalid_base_preference_controller_constructor")
                .that(grandfather)
                .isEmpty();
    }

    private static boolean hasValidConstructorSignature(Constructor constructor) {
        final Class[] parameterTypes = constructor.getParameterTypes();
        if (parameterTypes.length == 1) {
            return Context.class.isAssignableFrom(parameterTypes[0]);
        } else if (parameterTypes.length == 2) {
            return Context.class.isAssignableFrom(parameterTypes[0])
                    && String.class.isAssignableFrom(parameterTypes[1]);
        }
        return false;
    }
}
