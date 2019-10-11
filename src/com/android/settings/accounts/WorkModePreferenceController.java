/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.accounts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.SliceData;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class WorkModePreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "WorkModeController";

    private UserManager mUserManager;
    private UserHandle mManagedUser;

    private Preference mPreference;
    private IntentFilter mIntentFilter;

    public WorkModePreferenceController(Context context, String key) {
        super(context, key);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        mIntentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
    }

    public void setManagedUser(UserHandle managedUser) {
        mManagedUser = managedUser;
    }

    @Override
    public void onStart() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public int getAvailabilityStatus() {
        return (mManagedUser != null) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(isChecked()
                ? R.string.work_mode_on_summary
                : R.string.work_mode_off_summary);
    }

    private boolean isChecked() {
        boolean isWorkModeOn = false;
        if (mUserManager != null && mManagedUser != null) {
            isWorkModeOn = !mUserManager.isQuietModeEnabled(mManagedUser);
        }
        return isWorkModeOn;
    }

    private boolean setChecked(boolean isChecked) {
        if (mUserManager != null && mManagedUser != null) {
            final boolean quietModeEnabled = !isChecked;
            mUserManager.requestQuietModeEnabled(quietModeEnabled, mManagedUser);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof TwoStatePreference) {
            ((TwoStatePreference) preference).setChecked(isChecked());
        }
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        return setChecked((boolean) newValue);
    }

    /**
     * Receiver that listens to {@link Intent#ACTION_MANAGED_PROFILE_AVAILABLE} and
     * {@link Intent#ACTION_MANAGED_PROFILE_UNAVAILABLE}, and updates the work mode
     */
    @VisibleForTesting
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            final String action = intent.getAction();
            Log.v(TAG, "Received broadcast: " + action);

            if (Intent.ACTION_MANAGED_PROFILE_AVAILABLE.equals(action)
                    || Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE.equals(action)) {
                if (intent.getIntExtra(Intent.EXTRA_USER_HANDLE,
                        UserHandle.USER_NULL) == mManagedUser.getIdentifier()) {
                    updateState(mPreference);
                }
                return;
            }
            Log.w(TAG, "Cannot handle received broadcast: " + intent.getAction());
        }
    };

    @Override
    @SliceData.SliceType
    public int getSliceType() {
        return SliceData.SliceType.SWITCH;
    }
}