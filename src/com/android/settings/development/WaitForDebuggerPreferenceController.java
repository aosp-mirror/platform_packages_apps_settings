/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static com.android.settings.development.DevelopmentOptionsActivityRequestCodes
        .REQUEST_CODE_DEBUG_APP;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

public class WaitForDebuggerPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String WAIT_FOR_DEBUGGER_KEY = "wait_for_debugger";

    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    private SwitchPreference mPreference;

    public WaitForDebuggerPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return WAIT_FOR_DEBUGGER_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean debuggerEnabled = (Boolean) newValue;
        final String debugApp = Settings.Global.getString(
                mContext.getContentResolver(), Settings.Global.DEBUG_APP);
        writeDebuggerAppOptions(debugApp, debuggerEnabled, true /* persistent */);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final String debugApp = Settings.Global.getString(
                mContext.getContentResolver(), Settings.Global.DEBUG_APP);
        final boolean debuggerEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WAIT_FOR_DEBUGGER, SETTING_VALUE_OFF) != SETTING_VALUE_OFF;
        writeDebuggerAppOptions(debugApp, debuggerEnabled, true /* persistent */);
        mPreference.setChecked(debuggerEnabled);
        mPreference.setEnabled(!TextUtils.isEmpty(debugApp));
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_DEBUG_APP || resultCode != Activity.RESULT_OK) {
            return false;
        }
        final boolean debuggerEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WAIT_FOR_DEBUGGER, SETTING_VALUE_OFF) != SETTING_VALUE_OFF;
        final String debugApp = data.getAction();
        writeDebuggerAppOptions(debugApp, debuggerEnabled, true /* persistent */);
        mPreference.setChecked(debuggerEnabled);
        mPreference.setEnabled(!TextUtils.isEmpty(debugApp));
        return true;
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        writeDebuggerAppOptions(null /* package name */,
                false /* waitForDebugger */, false /* persistent */);
        mPreference.setChecked(false);
        mPreference.setEnabled(false);
    }

    @VisibleForTesting
    IActivityManager getActivityManagerService() {
        return ActivityManager.getService();
    }

    private void writeDebuggerAppOptions(String packageName, boolean waitForDebugger,
            boolean persistent) {
        try {
            getActivityManagerService().setDebugApp(packageName, waitForDebugger, persistent);
        } catch (RemoteException ex) {
            /* intentional no-op */
        }
    }
}
