/*
 * Copyright (C) 2008 The Android Open Source Project
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
import com.android.settings.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.List;

public class InputMethodAndLanguageSettings extends SettingsPreferenceFragment {

    private static final String KEY_PHONE_LANGUAGE = "phone_language";
    private static final String KEY_INPUT_METHOD = "input_method";
    private static final String KEY_KEYBOARD_SETTINGS_CATEGORY = "keyboard_settings_category";
    private static final String KEY_HARDKEYBOARD_CATEGORY = "hardkeyboard_category";
    private boolean mHaveHardKeyboard;

    private List<InputMethodInfo> mInputMethodProperties;
    private List<CheckBoxPreference> mCheckboxes;
    private Preference mLanguagePref;

    final TextUtils.SimpleStringSplitter mStringColonSplitter
            = new TextUtils.SimpleStringSplitter(':');

    private AlertDialog mDialog = null;
    
    static public String getInputMethodIdFromKey(String key) {
        return key;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.language_settings);

        if (getActivity().getAssets().getLocales().length == 1) {
            getPreferenceScreen().
                removePreference(findPreference(KEY_PHONE_LANGUAGE));
        } else {
            mLanguagePref = findPreference(KEY_PHONE_LANGUAGE);
        }

        Configuration config = getResources().getConfiguration();
        if (config.keyboard != Configuration.KEYBOARD_QWERTY) {
            getPreferenceScreen().removePreference(
                    getPreferenceScreen().findPreference(KEY_HARDKEYBOARD_CATEGORY));
        } else {
            mHaveHardKeyboard = true;
        }
        mCheckboxes = new ArrayList<CheckBoxPreference>();
        onCreateIMM();
    }

    private boolean isSystemIme(InputMethodInfo property) {
        return (property.getServiceInfo().applicationInfo.flags
                & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private void onCreateIMM() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);

        mInputMethodProperties = imm.getInputMethodList();

        PreferenceGroup keyboardSettingsCategory = (PreferenceGroup) findPreference(
                KEY_KEYBOARD_SETTINGS_CATEGORY);
        
        int N = (mInputMethodProperties == null ? 0 : mInputMethodProperties
                .size());
        for (int i = 0; i < N; ++i) {
            InputMethodInfo property = mInputMethodProperties.get(i);
            String prefKey = property.getId();

            CharSequence label = property.loadLabel(getActivity().getPackageManager());
            boolean systemIME = isSystemIme(property);
            // Add a check box.
            // Don't show the toggle if it's the only keyboard in the system, or it's a system IME.
            if (mHaveHardKeyboard || (N > 1 && !systemIME)) {
                CheckBoxPreference chkbxPref = new CheckBoxPreference(getActivity());
                chkbxPref.setKey(prefKey);
                chkbxPref.setTitle(label);
                keyboardSettingsCategory.addPreference(chkbxPref);
                mCheckboxes.add(chkbxPref);
            }

            // If setting activity is available, add a setting screen entry.
            if (null != property.getSettingsActivity()) {
                PreferenceScreen prefScreen = new PreferenceScreen(getActivity(), null);
                String settingsActivity = property.getSettingsActivity();
                if (settingsActivity.lastIndexOf("/") < 0) {
                    settingsActivity = property.getPackageName() + "/" + settingsActivity;
                }
                prefScreen.setKey(settingsActivity);
                prefScreen.setTitle(label);
                if (N == 1) {
                    prefScreen.setSummary(getResources().getString(
                            R.string.onscreen_keyboard_settings_summary));
                } else {
                    CharSequence settingsLabel = getResources().getString(
                            R.string.input_methods_settings_label_format, label);
                    prefScreen.setSummary(settingsLabel);
                }
                keyboardSettingsCategory.addPreference(prefScreen);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(
                this, getContentResolver(), mInputMethodProperties);

        if (mLanguagePref != null) {
            Configuration conf = getResources().getConfiguration();
            String locale = conf.locale.getDisplayName(conf.locale);
            if (locale != null && locale.length() > 1) {
                locale = Character.toUpperCase(locale.charAt(0)) + locale.substring(1);
                mLanguagePref.setSummary(locale);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(),
                mInputMethodProperties, mHaveHardKeyboard);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        // Input Method stuff
        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (preference instanceof CheckBoxPreference) {
            final CheckBoxPreference chkPref = (CheckBoxPreference) preference;
            final String id = getInputMethodIdFromKey(chkPref.getKey());
            if (chkPref.isChecked()) {
                InputMethodInfo selImi = null;
                final int N = mInputMethodProperties.size();
                for (int i=0; i<N; i++) {
                    InputMethodInfo imi = mInputMethodProperties.get(i);
                    if (id.equals(imi.getId())) {
                        selImi = imi;
                        if (isSystemIme(imi)) {
                            // This is a built-in IME, so no need to warn.
                            return super.onPreferenceTreeClick(preferenceScreen, preference);
                        }
                    }
                }
                chkPref.setChecked(false);
                if (selImi == null) {
                    return super.onPreferenceTreeClick(preferenceScreen, preference);
                }
                if (mDialog == null) {
                    // TODO: DialogFragment?
                    mDialog = (new AlertDialog.Builder(getActivity()))
                            .setTitle(android.R.string.dialog_alert_title)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            chkPref.setChecked(true);
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
                mDialog.setMessage(getResources().getString(
                        R.string.ime_security_warning,
                        selImi.getServiceInfo().applicationInfo.loadLabel(getPackageManager())));
                mDialog.show();
            }
        } else if (preference instanceof PreferenceScreen) {
            if (preference.getFragment() != null) {
                // Fragment will be handled correctly by the super class.
            } else if (KEY_INPUT_METHOD.equals(preference.getKey())) {
                final InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showInputMethodSubtypePicker();
            } else if (preference.getIntent() == null) {
                PreferenceScreen pref = (PreferenceScreen) preference;
                String activityName = pref.getKey();
                String packageName = activityName.substring(0, activityName
                        .lastIndexOf("."));
                int slash = activityName.indexOf("/");
                if (slash > 0) {
                    packageName = activityName.substring(0, slash);
                    activityName = activityName.substring(slash + 1);
                }
                if (activityName.length() > 0) {
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.setClassName(packageName, activityName);
                    startActivity(i);
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

}
