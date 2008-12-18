/*
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.settings;

import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

/*
 * Displays preferences for input methods.
 */
public class InputMethodsSettings extends PreferenceActivity {
    private List<InputMethodInfo> mInputMethodProperties;

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

        addPreferencesFromResource(R.xml.input_methods_prefs);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mInputMethodProperties = imm.getInputMethodList();

        mLastInputMethodId = Settings.Secure.getString(getContentResolver(),
            Settings.Secure.DEFAULT_INPUT_METHOD);

        int N = (mInputMethodProperties == null ? 0 : mInputMethodProperties
                .size());
        for (int i = 0; i < N; ++i) {
            InputMethodInfo property = mInputMethodProperties.get(i);
            String prefKey = property.getId();

            CharSequence label = property.loadLabel(getPackageManager());
            
            // Add a check box.
            CheckBoxPreference chkbxPref = new CheckBoxPreference(this);
            chkbxPref.setKey(prefKey);
            chkbxPref.setTitle(label);
            getPreferenceScreen().addPreference(chkbxPref);

            // If setting activity is available, add a setting screen entry.
            if (null != property.getSettingsActivity()) {
                PreferenceScreen prefScreen = new PreferenceScreen(this, null);
                prefScreen.setKey(property.getSettingsActivity());
                // XXX TODO: handle localization properly.
                prefScreen.setTitle(label + " settings");
                getPreferenceScreen().addPreference(prefScreen);
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
            pref.setChecked(enabled.contains(id));
        }
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
            if (pref.isChecked()) {
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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

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

        return false;
    }
}
