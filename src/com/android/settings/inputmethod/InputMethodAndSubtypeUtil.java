/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.inputmethod;

import com.android.settings.SettingsPreferenceFragment;

import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import java.util.HashSet;
import java.util.List;

public class InputMethodAndSubtypeUtil {

    private static final TextUtils.SimpleStringSplitter sStringColonSplitter
            = new TextUtils.SimpleStringSplitter(':');

    public static void saveInputMethodSubtypeList(
            SettingsPreferenceFragment context, ContentResolver resolver,
            List<InputMethodInfo> inputMethodProperties,
            boolean hasHardKeyboard, String lastTickedInputMethodId) {
        String lastInputMethodId = Settings.Secure.getString(resolver,
                Settings.Secure.DEFAULT_INPUT_METHOD);

        StringBuilder builder = new StringBuilder();
        StringBuilder disabledSysImes = new StringBuilder();

        int firstEnabled = -1;
        int N = inputMethodProperties.size();
        for (int i = 0; i < N; ++i) {
            final InputMethodInfo property = inputMethodProperties.get(i);
            final String id = property.getId();
            CheckBoxPreference pref = (CheckBoxPreference) context.findPreference(id);
            boolean currentInputMethod = id.equals(lastInputMethodId);
            boolean systemIme = isSystemIme(property);
            // TODO: Append subtypes by using the separator ";"
            if (((N == 1 || systemIme) && !hasHardKeyboard)
                    || (pref != null && pref.isChecked())) {
                if (builder.length() > 0) builder.append(':');
                builder.append(id);
                if (firstEnabled < 0) {
                    firstEnabled = i;
                }
            } else if (currentInputMethod) {
                lastInputMethodId = lastTickedInputMethodId;
            }
            // If it's a disabled system ime, add it to the disabled list so that it
            // doesn't get enabled automatically on any changes to the package list
            if (pref != null && !pref.isChecked() && systemIme && hasHardKeyboard) {
                if (disabledSysImes.length() > 0) disabledSysImes.append(":");
                disabledSysImes.append(id);
            }
        }

        // If the last input method is unset, set it as the first enabled one.
        if (TextUtils.isEmpty(lastInputMethodId)) {
            if (firstEnabled >= 0) {
                lastInputMethodId = inputMethodProperties.get(firstEnabled).getId();
            } else {
                lastInputMethodId = null;
            }
        }

        Settings.Secure.putString(resolver,
                Settings.Secure.ENABLED_INPUT_METHODS, builder.toString());
        Settings.Secure.putString(resolver,
                Settings.Secure.DISABLED_SYSTEM_INPUT_METHODS, disabledSysImes.toString());
        Settings.Secure.putString(resolver, Settings.Secure.DEFAULT_INPUT_METHOD,
                lastInputMethodId != null ? lastInputMethodId : "");
    }

    public static void loadInputMethodSubtypeList(
            SettingsPreferenceFragment context, ContentResolver resolver,
            List<InputMethodInfo> inputMethodProperties) {
        final HashSet<String> enabled = new HashSet<String>();
        String enabledStr = Settings.Secure.getString(resolver,
                Settings.Secure.ENABLED_INPUT_METHODS);
        if (enabledStr != null) {
            final TextUtils.SimpleStringSplitter splitter = sStringColonSplitter;
            splitter.setString(enabledStr);
            while (splitter.hasNext()) {
                enabled.add(splitter.next());
            }
        }

        // Update the statuses of the Check Boxes.
        int N = inputMethodProperties.size();
        // TODO: Use iterator.
        for (int i = 0; i < N; ++i) {
            final String id = inputMethodProperties.get(i).getId();
            CheckBoxPreference pref = (CheckBoxPreference) context.findPreference(
                    inputMethodProperties.get(i).getId());
            if (pref != null) {
                boolean isEnabled = enabled.contains(id);
                pref.setChecked(isEnabled);
                setSubtypesPreferenceEnabled(context, inputMethodProperties, id, isEnabled);
            }
        }
    }

    public static void setSubtypesPreferenceEnabled(SettingsPreferenceFragment context,
            List<InputMethodInfo> inputMethodProperties, String id, boolean enabled) {
        PreferenceScreen preferenceScreen = context.getPreferenceScreen();
        final int N = inputMethodProperties.size();
        // TODO: Use iterator.
        for (int i = 0; i < N; i++) {
            InputMethodInfo imi = inputMethodProperties.get(i);
            if (id.equals(imi.getId())) {
                for (InputMethodSubtype subtype: imi.getSubtypes()) {
                    preferenceScreen.findPreference(id + subtype.hashCode()).setEnabled(enabled);
                }
            }
        }
    }
    public static boolean isSystemIme(InputMethodInfo property) {
        return (property.getServiceInfo().applicationInfo.flags
                & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
}
