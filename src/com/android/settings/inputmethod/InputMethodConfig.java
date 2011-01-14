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

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InputMethodConfig extends SettingsPreferenceFragment {

    private static final String[] sSystemSettingNames = {
        System.TEXT_AUTO_REPLACE, System.TEXT_AUTO_CAPS, System.TEXT_AUTO_PUNCTUATE,
    };

    private static final String[] sHardKeyboardKeys = {
        "auto_replace", "auto_caps", "auto_punctuate",
    };

    private AlertDialog mDialog = null;
    private boolean mHaveHardKeyboard;
    private PreferenceCategory mHardKeyboardCategory;
    // Map of imi and its preferences
    final private HashMap<String, List<Preference>> mInputMethodPrefsMap =
            new HashMap<String, List<Preference>>();
    final private HashMap<InputMethodInfo, Preference> mActiveInputMethodsPrefMap =
            new HashMap<InputMethodInfo, Preference>();
    private List<InputMethodInfo> mInputMethodProperties;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Configuration config = getResources().getConfiguration();
        mHaveHardKeyboard = (config.keyboard == Configuration.KEYBOARD_QWERTY);
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);

        // TODO: Change mInputMethodProperties to Map
        mInputMethodProperties = imm.getInputMethodList();
        setPreferenceScreen(createPreferenceHierarchy());
    }

    @Override
    public void onResume() {
        super.onResume();

        ContentResolver resolver = getContentResolver();
        if (mHaveHardKeyboard) {
            for (int i = 0; i < sHardKeyboardKeys.length; ++i) {
                CheckBoxPreference chkPref = (CheckBoxPreference)
                        mHardKeyboardCategory.findPreference(sHardKeyboardKeys[i]);
                chkPref.setChecked(System.getInt(resolver, sSystemSettingNames[i], 1) > 0);
            }
        }

        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(
                this, resolver, mInputMethodProperties, mInputMethodPrefsMap);
        updateActiveInputMethodsSummary();
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(),
                mInputMethodProperties, mHaveHardKeyboard);
    }

    private void showSecurityWarnDialog(InputMethodInfo imi, final CheckBoxPreference chkPref,
            final String imiId) {
        if (mDialog == null) {
            mDialog = (new AlertDialog.Builder(getActivity()))
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            chkPref.setChecked(true);
                            for (Preference pref: mInputMethodPrefsMap.get(imiId)) {
                                pref.setEnabled(true);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create();
        } else {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }
        }
        mDialog.setMessage(getResources().getString(R.string.ime_security_warning,
                imi.getServiceInfo().applicationInfo.loadLabel(getPackageManager())));
        mDialog.show();
    }

    private InputMethodInfo getInputMethodInfoFromImiId(String imiId) {
        final int N = mInputMethodProperties.size();
        for (int i = 0; i < N; ++i) {
            InputMethodInfo imi = mInputMethodProperties.get(i);
            if (imiId.equals(imi.getId())) {
                return imi;
            }
        }
        return null;
    }

    @Override
    public boolean onPreferenceTreeClick(
            PreferenceScreen preferenceScreen, Preference preference) {

        if (preference instanceof CheckBoxPreference) {
            final CheckBoxPreference chkPref = (CheckBoxPreference) preference;

            if (mHaveHardKeyboard) {
                for (int i = 0; i < sHardKeyboardKeys.length; ++i) {
                    if (chkPref == mHardKeyboardCategory.findPreference(sHardKeyboardKeys[i])) {
                        System.putInt(getContentResolver(), sSystemSettingNames[i],
                                chkPref.isChecked() ? 1 : 0);
                        return true;
                    }
                }
            }

            final String imiId = chkPref.getKey();
            if (chkPref.isChecked()) {
                InputMethodInfo selImi = getInputMethodInfoFromImiId(imiId);
                if (selImi != null) {
                    if (InputMethodAndSubtypeUtil.isSystemIme(selImi)) {
                        // This is a built-in IME, so no need to warn.
                        return super.onPreferenceTreeClick(preferenceScreen, preference);
                    }
                } else {
                    return super.onPreferenceTreeClick(preferenceScreen, preference);
                }
                chkPref.setChecked(false);
                showSecurityWarnDialog(selImi, chkPref, imiId);
            } else {
                for (Preference pref: mInputMethodPrefsMap.get(imiId)) {
                    pref.setEnabled(false);
                }
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private void addInputMethodPreference(PreferenceScreen root, InputMethodInfo imi,
            final int imiSize) {
        PreferenceCategory keyboardSettingsCategory = new PreferenceCategory(getActivity());
        root.addPreference(keyboardSettingsCategory);
        final String imiId = imi.getId();
        mInputMethodPrefsMap.put(imiId, new ArrayList<Preference>());

        PackageManager pm = getPackageManager();
        CharSequence label = imi.loadLabel(pm);
        keyboardSettingsCategory.setTitle(label);

        final boolean isSystemIME = InputMethodAndSubtypeUtil.isSystemIme(imi);
        // Add a check box for enabling/disabling IME
        CheckBoxPreference chkbxPref = new CheckBoxPreference(getActivity());
        chkbxPref.setKey(imiId);
        chkbxPref.setTitle(label);
        keyboardSettingsCategory.addPreference(chkbxPref);
        // Disable the toggle if it's the only keyboard in the system, or it's a system IME.
        if (!mHaveHardKeyboard && (imiSize <= 1 || isSystemIME)) {
            chkbxPref.setEnabled(false);
        }

        Intent intent;
        // Add subtype settings when this IME has two or more subtypes.
        PreferenceScreen prefScreen = new PreferenceScreen(getActivity(), null);
        prefScreen.setTitle(R.string.active_input_method_subtypes);
        if (imi.getSubtypes().size() > 1) {
            intent = new Intent(Settings.ACTION_INPUT_METHOD_AND_SUBTYPE_ENABLER);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(InputMethodAndSubtypeEnabler.EXTRA_INPUT_METHOD_ID, imiId);
            prefScreen.setIntent(intent);
            keyboardSettingsCategory.addPreference(prefScreen);
            mActiveInputMethodsPrefMap.put(imi, prefScreen);
            mInputMethodPrefsMap.get(imiId).add(prefScreen);
        }

        // Add IME settings
        String settingsActivity = imi.getSettingsActivity();
        if (!TextUtils.isEmpty(settingsActivity)) {
            prefScreen = new PreferenceScreen(getActivity(), null);
            prefScreen.setTitle(R.string.input_method_settings);
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(imi.getPackageName(), settingsActivity);
            prefScreen.setIntent(intent);
            keyboardSettingsCategory.addPreference(prefScreen);
            mInputMethodPrefsMap.get(imiId).add(prefScreen);
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        addPreferencesFromResource(R.xml.hard_keyboard_settings);
        PreferenceScreen root = getPreferenceScreen();

        if (mHaveHardKeyboard) {
            mHardKeyboardCategory = (PreferenceCategory) findPreference("hard_keyboard");
        } else {
            root.removeAll();
        }

        final int N = (mInputMethodProperties == null ? 0 : mInputMethodProperties.size());
        for (int i = 0; i < N; ++i) {
            addInputMethodPreference(root, mInputMethodProperties.get(i), N);
        }
        return root;
    }

    private void updateActiveInputMethodsSummary() {
        final InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final PackageManager pm = getPackageManager();
        for (InputMethodInfo imi: mActiveInputMethodsPrefMap.keySet()) {
            Preference pref = mActiveInputMethodsPrefMap.get(imi);
            List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
            StringBuilder summary = new StringBuilder();
            boolean subtypeAdded = false;
            for (InputMethodSubtype subtype: subtypes) {
                if (subtypeAdded) {
                    summary.append(", ");
                }
                summary.append(pm.getText(imi.getPackageName(), subtype.getNameResId(),
                        imi.getServiceInfo().applicationInfo));
                subtypeAdded = true;
            }
            pref.setSummary(summary.toString());
        }
    }
}
