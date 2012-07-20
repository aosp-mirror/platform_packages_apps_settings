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

    private static final String KEY_ACTIVATE_ON_DOCK = "activate_on_dock";

    private CheckBoxPreference mActivateOnDockPreference;

    private Switch mEnableSwitch;
    private Enabler mEnabler;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addPreferencesFromResource(R.xml.dream_settings);

        mActivateOnDockPreference = (CheckBoxPreference) findPreference(KEY_ACTIVATE_ON_DOCK);

        final Activity activity = getActivity();

        mEnableSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            // note: we do not check onIsHidingHeaders() or onIsMultiPane() because there's no
            // switch in the left-hand pane to control this; we need to show the ON/OFF in our
            // fragment every time
            final int padding = activity.getResources().getDimensionPixelSize(
                    R.dimen.action_bar_switch_padding);
            mEnableSwitch.setPadding(0, 0, padding, 0);
            activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            activity.getActionBar().setCustomView(mEnableSwitch, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.END));
            activity.getActionBar().setTitle(R.string.screensaver_settings_title);
        }

        mEnabler = new Enabler(activity, mEnableSwitch);
    }

    public static boolean isScreenSaverEnabled(Context context) {
        return 0 != Settings.Secure.getInt(
                    context.getContentResolver(), SCREENSAVER_ENABLED, 1);
    }

    public static void setScreenSaverEnabled(Context context, boolean enabled) {
        Settings.Secure.putInt(
                context.getContentResolver(), SCREENSAVER_ENABLED, enabled ? 1 : 0);
    }

    public static class Enabler implements CompoundButton.OnCheckedChangeListener  {
        private final Context mContext;
        private Switch mSwitch;

        public Enabler(Context context, Switch switch_) {
            mContext = context;
            setSwitch(switch_);
        }
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            setScreenSaverEnabled(mContext, isChecked);
        }
        public void setSwitch(Switch switch_) {
            if (mSwitch == switch_) return;
            if (mSwitch != null) mSwitch.setOnCheckedChangeListener(null);
            mSwitch = switch_;
            mSwitch.setOnCheckedChangeListener(this);

            final boolean enabled = isScreenSaverEnabled(mContext);
            mSwitch.setChecked(enabled);
        }
        public void pause() {
            mSwitch.setOnCheckedChangeListener(null);
        }
        public void resume() {
            mSwitch.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        if (mEnabler != null) {
            mEnabler.resume();
        }

        final boolean currentActivateOnDock = 0 != Settings.Secure.getInt(getContentResolver(),
                SCREENSAVER_ACTIVATE_ON_DOCK, 1);
        mActivateOnDockPreference.setChecked(currentActivateOnDock);
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mEnabler != null) {
            mEnabler.pause();
        }

        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mActivateOnDockPreference) {
            Settings.Secure.putInt(getContentResolver(),
                    SCREENSAVER_ACTIVATE_ON_DOCK, 
                    mActivateOnDockPreference.isChecked() ? 1 : 0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
