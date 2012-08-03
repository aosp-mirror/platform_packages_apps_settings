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

package com.android.settings;

import static android.provider.Settings.Secure.SCREENSAVER_ENABLED;
import static android.provider.Settings.Secure.SCREENSAVER_ACTIVATE_ON_DOCK;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.IWindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;

public class DreamSettings extends SettingsPreferenceFragment {
    private static final String TAG = "DreamSettings";

    private static final String KEY_ACTIVATE_ON_SLEEP = "activate_on_sleep";
    private static final String KEY_ACTIVATE_ON_DOCK = "activate_on_dock";
    private static final String KEY_COMPONENT = "screensaver_component";
    private static final String KEY_TEST = "test";

    private static final int DEFAULT_SLEEP = 0;
    private static final int DEFAULT_DOCK = 1;

    private ActivationSetting mActivateOnSleep;
    private ActivationSetting mActivateOnDock;

    private Preference mComponentPref;
    private Preference mTestPref;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addPreferencesFromResource(R.xml.dream_settings);

        mComponentPref = findPreference(KEY_COMPONENT);
        mTestPref = findPreference(KEY_TEST);

        mActivateOnSleep = new ActivationSetting(getActivity(),
                SCREENSAVER_ENABLED, DEFAULT_SLEEP,
                (CheckBoxPreference) findPreference(KEY_ACTIVATE_ON_SLEEP));
        mActivateOnDock = new ActivationSetting(getActivity(),
                SCREENSAVER_ACTIVATE_ON_DOCK, DEFAULT_DOCK,
                (CheckBoxPreference) findPreference(KEY_ACTIVATE_ON_DOCK));
    }

    public static boolean isScreenSaverActivatedOnSleep(Context context) {
        return 0 != Settings.Secure.getInt(
                    context.getContentResolver(), SCREENSAVER_ENABLED, DEFAULT_SLEEP);
    }

    public static boolean isScreenSaverActivatedOnDock(Context context) {
        return 0 != Settings.Secure.getInt(
                    context.getContentResolver(), SCREENSAVER_ACTIVATE_ON_DOCK, DEFAULT_DOCK);
    }

    @Override
    public void onResume() {
        mActivateOnSleep.onResume();
        mActivateOnDock.onResume();
        refreshDependents();
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        mActivateOnSleep.onClick(preference);
        mActivateOnDock.onClick(preference);
        refreshDependents();
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void refreshDependents() {
        boolean enabled = mActivateOnSleep.isSelected() || mActivateOnDock.isSelected();
        mComponentPref.setEnabled(enabled);
        mTestPref.setEnabled(enabled);
    }

    private static class ActivationSetting {
        private final Context mContext;
        private final String mName;
        private final int mDefaultValue;
        private final CheckBoxPreference mPref;

        ActivationSetting(Context context, String name, int defaultValue, CheckBoxPreference pref) {
            mContext = context;
            mName = name;
            mDefaultValue = defaultValue;
            mPref = pref;
        }
        public boolean isSelected() {
            return mPref.isChecked();
        }
        void onClick(Preference preference) {
            if (preference == mPref) {
                Settings.Secure.putInt(mContext.getContentResolver(),
                        mName,
                        mPref.isChecked() ? 1 : 0);
            }
        }
        void onResume() {
            boolean currentActivated = 0 != Settings.Secure.getInt(mContext.getContentResolver(),
                    mName, mDefaultValue);
            mPref.setChecked(currentActivated);
        }
    }

}
