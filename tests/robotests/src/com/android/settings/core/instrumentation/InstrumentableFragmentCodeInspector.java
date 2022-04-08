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

package com.android.settings.core.instrumentation;

import static com.google.common.truth.Truth.assertWithMessage;

import android.util.ArraySet;

import androidx.fragment.app.Fragment;

import com.android.settings.core.codeinspection.CodeInspector;
import com.android.settingslib.core.instrumentation.Instrumentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@link CodeInspector} that verifies all fragments implements Instrumentable.
 */
public class InstrumentableFragmentCodeInspector extends CodeInspector {

    private final List<String> grandfather_notImplementingInstrumentable;

    public InstrumentableFragmentCodeInspector(List<Class<?>> classes) {
        super(classes);
        grandfather_notImplementingInstrumentable = new ArrayList<>();
        initializeGrandfatherList(grandfather_notImplementingInstrumentable,
                "grandfather_not_implementing_instrumentable");
    }

    @Override
    public void run() {
        final Set<String> broken = new ArraySet<>();

        for (Class clazz : mClasses) {
            if (!isConcreteSettingsClass(clazz)) {
                continue;
            }
            final String className = clazz.getName();
            // If it's a fragment, it must also be instrumentable.
            final boolean whitelisted =
                    grandfather_notImplementingInstrumentable.remove(className);
            if (Fragment.class.isAssignableFrom(clazz)
                    && !Instrumentable.class.isAssignableFrom(clazz)
                    && !whitelisted) {
                broken.add(className);
            }
        }
        final StringBuilder sb = new StringBuilder(
                "All fragments should implement Instrumentable, but the following are not:\n");
        for (String c : broken) {
            sb.append(c).append("\n");
        }
        assertWithMessage(sb.toString()).that(broken.isEmpty()).isTrue();
        assertNoObsoleteInGrandfatherList("grandfather_not_implementing_instrumentable",
                grandfather_notImplementingInstrumentable);
    }
}
