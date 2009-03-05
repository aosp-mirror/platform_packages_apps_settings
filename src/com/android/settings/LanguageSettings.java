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

package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LanguageSettings extends PreferenceActivity {
    
    private boolean mHaveHardKeyboard;

    private List<InputMethodInfo> mInputMethodProperties;
    private List<CheckBoxPreference> mCheckboxes;

    final TextUtils.SimpleStringSplitter mStringColonSplitter
            = new TextUtils.SimpleStringSplitter(':');
    
    private String mLastInputMethodId;
    private String mLastTickedInputMethodId;

    static public String getInputMethodIdFromKey(String key) {
        return key;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.language_settings);

        if (getAssets().getLocales().length == 1) {
            getPreferenceScreen().
                removePreference(findPreference("language_category"));
        }

        Configuration config = getResources().getConfiguration();
        if (config.keyboard != Configuration.KEYBOARD_QWERTY) {
            getPreferenceScreen().removePreference(
                    getPreferenceScreen().findPreference("hardkeyboard_category"));
        } else {
            mHaveHardKeyboard = true;
        }
        mCheckboxes = new ArrayList<CheckBoxPreference>();
        onCreateIMM();
    }
    
    private void onCreateIMM() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mInputMethodProperties = imm.getInputMethodList();

        mLastInputMethodId = Settings.Secure.getString(getContentResolver(),
            Settings.Secure.DEFAULT_INPUT_METHOD);
        
        PreferenceGroup textCategory = (PreferenceGroup) findPreference("text_category");
        
        int N = (mInputMethodProperties == null ? 0 : mInputMethodProperties
                .size());
        for (int i = 0; i < N; ++i) {
            InputMethodInfo property = mInputMethodProperties.get(i);
            String prefKey = property.getId();

            CharSequence label = property.loadLabel(getPackageManager());
            
            // Add a check box.
            // Don't show the toggle if it's the only keyboard in the system
            if (mHaveHardKeyboard || N > 1) {
                CheckBoxPreference chkbxPref = new CheckBoxPreference(this);
                chkbxPref.setKey(prefKey);
                chkbxPref.setTitle(label);
                textCategory.addPreference(chkbxPref);
                mCheckboxes.add(chkbxPref);
            }

            // If setting activity is available, add a setting screen entry.
            if (null != property.getSettingsActivity()) {
                PreferenceScreen prefScreen = new PreferenceScreen(this, null);
                prefScreen.setKey(property.getSettingsActivity());
                prefScreen.setTitle(label);
                if (N == 1) {
                    prefScreen.setSummary(getString(R.string.onscreen_keyboard_settings_summary));
                } else {
                    CharSequence settingsLabel = getResources().getString(
                            R.string.input_methods_settings_label_format, label);
                    prefScreen.setSummary(settingsLabel);
                }
                textCategory.addPreference(prefScreen);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        final HashSet<String> enabled = new HashSet<String>();
        String enabledStr = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_INPUT_METHODS);
        if (enabledStr != null) {
            final TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
            splitter.setString(enabledStr);
            while (splitter.hasNext()) {
                enabled.add(splitter.next());
            }
        }
        
        // Update the statuses of the Check Boxes.
        int N = mInputMethodProperties.size();
        for (int i = 0; i < N; ++i) {
            final String id = mInputMethodProperties.get(i).getId();
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(mInputMethodProperties
                    .get(i).getId());
            if (pref != null) {
                pref.setChecked(enabled.contains(id));
            }
        }
        updateCheckboxes();
        mLastTickedInputMethodId = null;
    }

    @Override
    protected void onPause() {
        super.onPause();

        StringBuilder builder = new StringBuilder(256);
        
        boolean haveLastInputMethod = false;
        
        int firstEnabled = -1;
        int N = mInputMethodProperties.size();
        for (int i = 0; i < N; ++i) {
            final String id = mInputMethodProperties.get(i).getId();
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(id);
            boolean hasIt = id.equals(mLastInputMethodId);
            if ((N == 1 && !mHaveHardKeyboard) || (pref != null && pref.isChecked())) {
                if (builder.length() > 0) builder.append(':');
                builder.append(id);
                if (firstEnabled < 0) {
                    firstEnabled = i;
                }
                if (hasIt) haveLastInputMethod = true;
            } else if (hasIt) {
                mLastInputMethodId = mLastTickedInputMethodId;
            }
        }

        // If the last input method is unset, set it as the first enabled one.
        if (null == mLastInputMethodId || "".equals(mLastInputMethodId)) {
            if (firstEnabled >= 0) {
                mLastInputMethodId = mInputMethodProperties.get(firstEnabled).getId();
            } else {
                mLastInputMethodId = null;
            }
        }
        
        Settings.Secure.putString(getContentResolver(),
            Settings.Secure.ENABLED_INPUT_METHODS, builder.toString());
        Settings.Secure.putString(getContentResolver(),
            Settings.Secure.DEFAULT_INPUT_METHOD, mLastInputMethodId);
    }

    private void updateCheckboxes() {
        final int count = mCheckboxes.size();
        int nChecked = 0;
        int iChecked = -1;
        // See how many are checked and note the only or last checked one
        for (int i = 0; i < count; i++) {
            if (mCheckboxes.get(i).isChecked()) {
                iChecked = i;
                nChecked++;
            }
        }
        // 
        if (nChecked == 1) {
            mCheckboxes.get(iChecked).setEnabled(false);
        } else {
            for (int i = 0; i < count; i++) {
                mCheckboxes.get(i).setEnabled(true);
            }
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        
        // Input Method stuff
        // Those monkeys kept committing suicide, so we add this property
        // to disable this functionality
        if (!TextUtils.isEmpty(SystemProperties.get("ro.monkey"))) {
            return false;
        }

        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference chkPref = (CheckBoxPreference) preference;
            String id = getInputMethodIdFromKey(chkPref.getKey());
            if (chkPref.isChecked()) {
                mLastTickedInputMethodId = id;
            } else if (id.equals(mLastTickedInputMethodId)) {
                mLastTickedInputMethodId = null;
            }
        } else if (preference instanceof PreferenceScreen) {
            if (preference.getIntent() == null) {
                PreferenceScreen pref = (PreferenceScreen) preference;
                String activityName = pref.getKey();
                String packageName = activityName.substring(0, activityName
                        .lastIndexOf("."));
                if (activityName.length() > 0) {
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.setClassName(packageName, activityName);
                    startActivity(i);
                }
            }
        }
        updateCheckboxes();
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
