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
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class InputMethodAndSubtypeUtil {

    private static final boolean DEBUG = false;
    static final String TAG = "InputMethdAndSubtypeUtil";

    private static final char INPUT_METHOD_SEPARATER = ':';
    private static final char INPUT_METHOD_SUBTYPE_SEPARATER = ';';
    private static final int NOT_A_SUBTYPE_ID = -1;

    private static final TextUtils.SimpleStringSplitter sStringInputMethodSplitter
            = new TextUtils.SimpleStringSplitter(INPUT_METHOD_SEPARATER);

    private static final TextUtils.SimpleStringSplitter sStringInputMethodSubtypeSplitter
            = new TextUtils.SimpleStringSplitter(INPUT_METHOD_SUBTYPE_SEPARATER);

    private static int getInputMethodSubtypeSelected(ContentResolver resolver) {
        try {
            return Settings.Secure.getInt(resolver,
                    Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE);
        } catch (SettingNotFoundException e) {
            return NOT_A_SUBTYPE_ID;
        }
    }

    private static boolean isInputMethodSubtypeSelected(ContentResolver resolver) {
        return getInputMethodSubtypeSelected(resolver) != NOT_A_SUBTYPE_ID;
    }

    private static void putSelectedInputMethodSubtype(ContentResolver resolver, int hashCode) {
        Settings.Secure.putInt(resolver, Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE, hashCode);
    }

    // Needs to modify InputMethodManageService if you want to change the format of saved string.
    private static HashMap<String, HashSet<String>> getEnabledInputMethodsAndSubtypeList(
            ContentResolver resolver) {
        final String enabledInputMethodsStr = Settings.Secure.getString(
                resolver, Settings.Secure.ENABLED_INPUT_METHODS);
        HashMap<String, HashSet<String>> imsList
                = new HashMap<String, HashSet<String>>();
        if (DEBUG) {
            Log.d(TAG, "--- Load enabled input methods: " + enabledInputMethodsStr);
        }

        if (TextUtils.isEmpty(enabledInputMethodsStr)) {
            return imsList;
        }
        sStringInputMethodSplitter.setString(enabledInputMethodsStr);
        while (sStringInputMethodSplitter.hasNext()) {
            String nextImsStr = sStringInputMethodSplitter.next();
            sStringInputMethodSubtypeSplitter.setString(nextImsStr);
            if (sStringInputMethodSubtypeSplitter.hasNext()) {
                HashSet<String> subtypeHashes = new HashSet<String>();
                // The first element is ime id.
                String imeId = sStringInputMethodSubtypeSplitter.next();
                while (sStringInputMethodSubtypeSplitter.hasNext()) {
                    subtypeHashes.add(sStringInputMethodSubtypeSplitter.next());
                }
                imsList.put(imeId, subtypeHashes);
            }
        }
        return imsList;
    }

    public static void saveInputMethodSubtypeList(
            SettingsPreferenceFragment context, ContentResolver resolver,
            List<InputMethodInfo> inputMethodProperties, boolean hasHardKeyboard) {
        String currentInputMethodId = Settings.Secure.getString(resolver,
                Settings.Secure.DEFAULT_INPUT_METHOD);
        final int selectedInputMethodSubtype = getInputMethodSubtypeSelected(resolver);

        StringBuilder builder = new StringBuilder();
        StringBuilder disabledSysImes = new StringBuilder();
        int firstSubtypeHashCode = NOT_A_SUBTYPE_ID;

        final boolean onlyOneIME = inputMethodProperties.size() == 1;
        boolean existsSelectedIME = false;
        for (InputMethodInfo property : inputMethodProperties) {
            final String id = property.getId();
            CheckBoxPreference pref = (CheckBoxPreference) context.findPreference(id);
            boolean isCurrentInputMethod = id.equals(currentInputMethodId);
            boolean systemIme = isSystemIme(property);
            // TODO: Append subtypes by using the separator ";"
            if (((onlyOneIME || systemIme) && !hasHardKeyboard)
                    || (pref != null && pref.isChecked())) {
                if (builder.length() > 0) builder.append(INPUT_METHOD_SEPARATER);
                builder.append(id);
                for (InputMethodSubtype subtype : property.getSubtypes()) {
                    CheckBoxPreference subtypePref = (CheckBoxPreference) context.findPreference(
                            id + subtype.hashCode());
                    if (subtypePref != null && subtypePref.isChecked()) {
                        builder.append(INPUT_METHOD_SUBTYPE_SEPARATER).append(subtype.hashCode());
                        if (isCurrentInputMethod) {
                            if (selectedInputMethodSubtype == subtype.hashCode()) {
                                existsSelectedIME = true;
                            } else if (firstSubtypeHashCode == NOT_A_SUBTYPE_ID) {
                                firstSubtypeHashCode = subtype.hashCode();
                            }
                        }
                    }
                }
            } else if (isCurrentInputMethod) {
                // We are processing the current input method, but found that it's not enabled.
                // This means that the current input method has been uninstalled.
                // If currentInputMethod is already uninstalled, InputMethodManagerService will
                // find the applicable IME from the history and the system locale.
                if (DEBUG) {
                    Log.d(TAG, "Current IME was uninstalled or disabled.");
                }
            }
            // If it's a disabled system ime, add it to the disabled list so that it
            // doesn't get enabled automatically on any changes to the package list
            if (pref != null && !pref.isChecked() && systemIme && hasHardKeyboard) {
                if (disabledSysImes.length() > 0) disabledSysImes.append(INPUT_METHOD_SEPARATER);
                disabledSysImes.append(id);
            }
        }

        if (DEBUG) {
            Log.d(TAG, "--- Save enabled inputmethod settings. :" + builder.toString());
            Log.d(TAG, "--- Save disable system inputmethod settings. :"
                    + disabledSysImes.toString());
            Log.d(TAG, "--- Save default inputmethod settings. :" + currentInputMethodId);
        }

        // Redefines SelectedSubtype when all subtypes are unchecked or there is no subtype
        // selected. And if the selected subtype of the current input method was disabled,
        // We should reset the selected input method's subtype.
        if (!existsSelectedIME || !isInputMethodSubtypeSelected(resolver)) {
            if (DEBUG) {
                Log.d(TAG, "--- Set inputmethod subtype because it's not defined."
                        + firstSubtypeHashCode);
            }
            putSelectedInputMethodSubtype(resolver, firstSubtypeHashCode);
        }

        Settings.Secure.putString(resolver,
                Settings.Secure.ENABLED_INPUT_METHODS, builder.toString());
        Settings.Secure.putString(resolver,
                Settings.Secure.DISABLED_SYSTEM_INPUT_METHODS, disabledSysImes.toString());
        // If the current input method is unset, InputMethodManagerService will find the applicable
        // IME from the history and the system locale.
        Settings.Secure.putString(resolver, Settings.Secure.DEFAULT_INPUT_METHOD,
                currentInputMethodId != null ? currentInputMethodId : "");
    }

    public static void loadInputMethodSubtypeList(
            SettingsPreferenceFragment context, ContentResolver resolver,
            List<InputMethodInfo> inputMethodProperties) {
        HashMap<String, HashSet<String>> enabledSubtypes =
                getEnabledInputMethodsAndSubtypeList(resolver);

        for (InputMethodInfo property : inputMethodProperties) {
            final String id = property.getId();
            CheckBoxPreference pref = (CheckBoxPreference) context.findPreference(id);
            if (pref != null) {
                boolean isEnabled = enabledSubtypes.containsKey(id);
                pref.setChecked(isEnabled);
                setSubtypesPreferenceEnabled(context, inputMethodProperties, id, isEnabled);
                updateSubtypesPreferenceChecked(context, inputMethodProperties, enabledSubtypes);
            }
        }
    }

    public static void setSubtypesPreferenceEnabled(SettingsPreferenceFragment context,
            List<InputMethodInfo> inputMethodProperties, String id, boolean enabled) {
        PreferenceScreen preferenceScreen = context.getPreferenceScreen();
        for (InputMethodInfo imi : inputMethodProperties) {
            if (id.equals(imi.getId())) {
                for (InputMethodSubtype subtype : imi.getSubtypes()) {
                    CheckBoxPreference pref = (CheckBoxPreference) preferenceScreen.findPreference(
                            id + subtype.hashCode());
                    if (pref != null) {
                        pref.setEnabled(enabled);
                    }
                }
            }
        }
    }

    public static void updateSubtypesPreferenceChecked(SettingsPreferenceFragment context,
            List<InputMethodInfo> inputMethodProperties,
            HashMap<String, HashSet<String>> enabledSubtypes) {
        PreferenceScreen preferenceScreen = context.getPreferenceScreen();
        for (InputMethodInfo imi : inputMethodProperties) {
            String id = imi.getId();
            HashSet<String> enabledSubtypesSet = enabledSubtypes.get(id);
            for (InputMethodSubtype subtype : imi.getSubtypes()) {
                String hashCode = String.valueOf(subtype.hashCode());
                if (DEBUG) {
                    Log.d(TAG, "--- Set checked state: " + "id" + ", " + hashCode + ", "
                            + enabledSubtypesSet.contains(hashCode));
                }
                CheckBoxPreference pref = (CheckBoxPreference) preferenceScreen.findPreference(
                        id + hashCode);
                if (pref != null) {
                    pref.setChecked(enabledSubtypesSet.contains(hashCode));
                }
            }
        }
    }

    public static boolean isSystemIme(InputMethodInfo property) {
        return (property.getServiceInfo().applicationInfo.flags
                & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
}
