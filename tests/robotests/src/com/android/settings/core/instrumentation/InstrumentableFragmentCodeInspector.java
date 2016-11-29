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

import android.app.Fragment;
import android.util.ArraySet;

import com.android.settings.ChooseLockPassword;
import com.android.settings.ChooseLockPattern;
import com.android.settings.CredentialCheckResultTracker;
import com.android.settings.CustomDialogPreference;
import com.android.settings.CustomEditTextPreference;
import com.android.settings.CustomListPreference;
import com.android.settings.RestrictedListPreference;
import com.android.settings.applications.AppOpsCategory;
import com.android.settings.core.codeinspection.CodeInspector;
import com.android.settings.core.lifecycle.ObservableDialogFragment;
import com.android.settings.deletionhelper.ActivationWarningFragment;
import com.android.settings.inputmethod.UserDictionaryLocalePicker;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link CodeInspector} that verifies all fragments implements Instrumentable.
 */
public class InstrumentableFragmentCodeInspector extends CodeInspector {

    private static final String TEST_CLASS_SUFFIX = "Test";

    private static final List<String> whitelist;

    static {
        whitelist = new ArrayList<>();
        whitelist.add(
                CustomEditTextPreference.CustomPreferenceDialogFragment.class.getName());
        whitelist.add(
                CustomListPreference.CustomListPreferenceDialogFragment.class.getName());
        whitelist.add(
                RestrictedListPreference.RestrictedListPreferenceDialogFragment.class.getName());
        whitelist.add(ChooseLockPassword.SaveAndFinishWorker.class.getName());
        whitelist.add(ChooseLockPattern.SaveAndFinishWorker.class.getName());
        whitelist.add(ActivationWarningFragment.class.getName());
        whitelist.add(ObservableDialogFragment.class.getName());
        whitelist.add(CustomDialogPreference.CustomPreferenceDialogFragment.class.getName());
        whitelist.add(AppOpsCategory.class.getName());
        whitelist.add(UserDictionaryLocalePicker.class.getName());
        whitelist.add(CredentialCheckResultTracker.class.getName());
    }

    public InstrumentableFragmentCodeInspector(List<Class<?>> classes) {
        super(classes);
    }

    @Override
    public void run() {
        final Set<String> broken = new ArraySet<>();

        for (Class clazz : mClasses) {
            // Skip abstract classes.
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            final String packageName = clazz.getPackage().getName();
            // Skip classes that are not in Settings.
            if (!packageName.contains(PACKAGE_NAME + ".")) {
                continue;
            }
            final String className = clazz.getName();
            // Skip classes from tests.
            if (className.endsWith(TEST_CLASS_SUFFIX)) {
                continue;
            }
            // If it's a fragment, it must also be instrumentable.
            if (Fragment.class.isAssignableFrom(clazz)
                    && !Instrumentable.class.isAssignableFrom(clazz)
                    && !whitelist.contains(className)) {
                broken.add(className);
            }
        }
        final StringBuilder sb = new StringBuilder(
                "All fragment should implement Instrumentable, but the following are not:\n");
        for (String c : broken) {
            sb.append(c).append("\n");
        }
        assertWithMessage(sb.toString())
                .that(broken.isEmpty())
                .isTrue();
    }
}
