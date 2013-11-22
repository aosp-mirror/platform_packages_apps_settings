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

import com.android.internal.inputmethod.InputMethodUtils;
import com.android.settings.SettingsPreferenceFragment;

import android.content.ContentResolver;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
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
import java.util.Map;

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

    private static void buildEnabledInputMethodsString(
            StringBuilder builder, String imi, HashSet<String> subtypes) {
        builder.append(imi);
        // Inputmethod and subtypes are saved in the settings as follows:
        // ime0;subtype0;subtype1:ime1;subtype0:ime2:ime3;subtype0;subtype1
        for (String subtypeId: subtypes) {
            builder.append(INPUT_METHOD_SUBTYPE_SEPARATER).append(subtypeId);
        }
    }

    public static void buildInputMethodsAndSubtypesString(
            StringBuilder builder, HashMap<String, HashSet<String>> imsList) {
        boolean needsAppendSeparator = false;
        for (String imi: imsList.keySet()) {
            if (needsAppendSeparator) {
                builder.append(INPUT_METHOD_SEPARATER);
            } else {
                needsAppendSeparator = true;
            }
            buildEnabledInputMethodsString(builder, imi, imsList.get(imi));
        }
    }

    public static void buildDisabledSystemInputMethods(
            StringBuilder builder, HashSet<String> imes) {
        boolean needsAppendSeparator = false;
        for (String ime: imes) {
            if (needsAppendSeparator) {
                builder.append(INPUT_METHOD_SEPARATER);
            } else {
                needsAppendSeparator = true;
            }
            builder.append(ime);
        }
    }

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

    private static HashSet<String> getDisabledSystemIMEs(ContentResolver resolver) {
        HashSet<String> set = new HashSet<String>();
        String disabledIMEsStr = Settings.Secure.getString(
                resolver, Settings.Secure.DISABLED_SYSTEM_INPUT_METHODS);
        if (TextUtils.isEmpty(disabledIMEsStr)) {
            return set;
        }
        sStringInputMethodSplitter.setString(disabledIMEsStr);
        while(sStringInputMethodSplitter.hasNext()) {
            set.add(sStringInputMethodSplitter.next());
        }
        return set;
    }

    public static void saveInputMethodSubtypeList(SettingsPreferenceFragment context,
            ContentResolver resolver, List<InputMethodInfo> inputMethodInfos,
            boolean hasHardKeyboard) {
        String currentInputMethodId = Settings.Secure.getString(resolver,
                Settings.Secure.DEFAULT_INPUT_METHOD);
        final int selectedInputMethodSubtype = getInputMethodSubtypeSelected(resolver);
        HashMap<String, HashSet<String>> enabledIMEAndSubtypesMap =
                getEnabledInputMethodsAndSubtypeList(resolver);
        HashSet<String> disabledSystemIMEs = getDisabledSystemIMEs(resolver);

        final int imiCount = inputMethodInfos.size();
        boolean needsToResetSelectedSubtype = false;
        for (InputMethodInfo imi : inputMethodInfos) {
            final String imiId = imi.getId();
            Preference pref = context.findPreference(imiId);
            if (pref == null) continue;
            // In the Configure input method screen or in the subtype enabler screen.
            // pref is instance of CheckBoxPreference in the Configure input method screen.
            final boolean isImeChecked = (pref instanceof CheckBoxPreference) ?
                    ((CheckBoxPreference) pref).isChecked()
                    : enabledIMEAndSubtypesMap.containsKey(imiId);
            final boolean isCurrentInputMethod = imiId.equals(currentInputMethodId);
            final boolean systemIme = InputMethodUtils.isSystemIme(imi);
            if ((!hasHardKeyboard && InputMethodSettingValuesWrapper.getInstance(
                    context.getActivity()).isAlwaysCheckedIme(imi, context.getActivity()))
                    || isImeChecked) {
                if (!enabledIMEAndSubtypesMap.containsKey(imiId)) {
                    // imiId has just been enabled
                    enabledIMEAndSubtypesMap.put(imiId, new HashSet<String>());
                }
                HashSet<String> subtypesSet = enabledIMEAndSubtypesMap.get(imiId);

                boolean subtypePrefFound = false;
                final int subtypeCount = imi.getSubtypeCount();
                for (int i = 0; i < subtypeCount; ++i) {
                    InputMethodSubtype subtype = imi.getSubtypeAt(i);
                    final String subtypeHashCodeStr = String.valueOf(subtype.hashCode());
                    CheckBoxPreference subtypePref = (CheckBoxPreference) context.findPreference(
                            imiId + subtypeHashCodeStr);
                    // In the Configure input method screen which does not have subtype preferences.
                    if (subtypePref == null) continue;
                    if (!subtypePrefFound) {
                        // Once subtype checkbox is found, subtypeSet needs to be cleared.
                        // Because of system change, hashCode value could have been changed.
                        subtypesSet.clear();
                        // If selected subtype preference is disabled, needs to reset.
                        needsToResetSelectedSubtype = true;
                        subtypePrefFound = true;
                    }
                    if (subtypePref.isChecked()) {
                        subtypesSet.add(subtypeHashCodeStr);
                        if (isCurrentInputMethod) {
                            if (selectedInputMethodSubtype == subtype.hashCode()) {
                                // Selected subtype is still enabled, there is no need to reset
                                // selected subtype.
                                needsToResetSelectedSubtype = false;
                            }
                        }
                    } else {
                        subtypesSet.remove(subtypeHashCodeStr);
                    }
                }
            } else {
                enabledIMEAndSubtypesMap.remove(imiId);
                if (isCurrentInputMethod) {
                    // We are processing the current input method, but found that it's not enabled.
                    // This means that the current input method has been uninstalled.
                    // If currentInputMethod is already uninstalled, InputMethodManagerService will
                    // find the applicable IME from the history and the system locale.
                    if (DEBUG) {
                        Log.d(TAG, "Current IME was uninstalled or disabled.");
                    }
                    currentInputMethodId = null;
                }
            }
            // If it's a disabled system ime, add it to the disabled list so that it
            // doesn't get enabled automatically on any changes to the package list
            if (systemIme && hasHardKeyboard) {
                if (disabledSystemIMEs.contains(imiId)) {
                    if (isImeChecked) {
                        disabledSystemIMEs.remove(imiId);
                    }
                } else {
                    if (!isImeChecked) {
                        disabledSystemIMEs.add(imiId);
                    }
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        buildInputMethodsAndSubtypesString(builder, enabledIMEAndSubtypesMap);
        StringBuilder disabledSysImesBuilder = new StringBuilder();
        buildDisabledSystemInputMethods(disabledSysImesBuilder, disabledSystemIMEs);
        if (DEBUG) {
            Log.d(TAG, "--- Save enabled inputmethod settings. :" + builder.toString());
            Log.d(TAG, "--- Save disable system inputmethod settings. :"
                    + disabledSysImesBuilder.toString());
            Log.d(TAG, "--- Save default inputmethod settings. :" + currentInputMethodId);
            Log.d(TAG, "--- Needs to reset the selected subtype :" + needsToResetSelectedSubtype);
            Log.d(TAG, "--- Subtype is selected :" + isInputMethodSubtypeSelected(resolver));
        }

        // Redefines SelectedSubtype when all subtypes are unchecked or there is no subtype
        // selected. And if the selected subtype of the current input method was disabled,
        // We should reset the selected input method's subtype.
        if (needsToResetSelectedSubtype || !isInputMethodSubtypeSelected(resolver)) {
            if (DEBUG) {
                Log.d(TAG, "--- Reset inputmethod subtype because it's not defined.");
            }
            putSelectedInputMethodSubtype(resolver, NOT_A_SUBTYPE_ID);
        }

        Settings.Secure.putString(resolver,
                Settings.Secure.ENABLED_INPUT_METHODS, builder.toString());
        if (disabledSysImesBuilder.length() > 0) {
            Settings.Secure.putString(resolver, Settings.Secure.DISABLED_SYSTEM_INPUT_METHODS,
                    disabledSysImesBuilder.toString());
        }
        // If the current input method is unset, InputMethodManagerService will find the applicable
        // IME from the history and the system locale.
        Settings.Secure.putString(resolver, Settings.Secure.DEFAULT_INPUT_METHOD,
                currentInputMethodId != null ? currentInputMethodId : "");
    }

    public static void loadInputMethodSubtypeList(
            SettingsPreferenceFragment context, ContentResolver resolver,
            List<InputMethodInfo> inputMethodInfos,
            final Map<String, List<Preference>> inputMethodPrefsMap) {
        HashMap<String, HashSet<String>> enabledSubtypes =
                getEnabledInputMethodsAndSubtypeList(resolver);

        for (InputMethodInfo imi : inputMethodInfos) {
            final String imiId = imi.getId();
            Preference pref = context.findPreference(imiId);
            if (pref != null && pref instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) pref;
                boolean isEnabled = enabledSubtypes.containsKey(imiId);
                checkBoxPreference.setChecked(isEnabled);
                if (inputMethodPrefsMap != null) {
                    for (Preference childPref: inputMethodPrefsMap.get(imiId)) {
                        childPref.setEnabled(isEnabled);
                    }
                }
                setSubtypesPreferenceEnabled(context, inputMethodInfos, imiId, isEnabled);
            }
        }
        updateSubtypesPreferenceChecked(context, inputMethodInfos, enabledSubtypes);
    }

    public static void setSubtypesPreferenceEnabled(SettingsPreferenceFragment context,
            List<InputMethodInfo> inputMethodProperties, String id, boolean enabled) {
        PreferenceScreen preferenceScreen = context.getPreferenceScreen();
        for (InputMethodInfo imi : inputMethodProperties) {
            if (id.equals(imi.getId())) {
                final int subtypeCount = imi.getSubtypeCount();
                for (int i = 0; i < subtypeCount; ++i) {
                    InputMethodSubtype subtype = imi.getSubtypeAt(i);
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
            if (!enabledSubtypes.containsKey(id)) break;
            final HashSet<String> enabledSubtypesSet = enabledSubtypes.get(id);
            final int subtypeCount = imi.getSubtypeCount();
            for (int i = 0; i < subtypeCount; ++i) {
                InputMethodSubtype subtype = imi.getSubtypeAt(i);
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
}
