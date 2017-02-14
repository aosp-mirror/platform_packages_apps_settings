/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.accounts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import static android.provider.Settings.Secure.MANAGED_PROFILE_CONTACT_REMOTE_SEARCH;

/**
 * Setting page for managed profile.
 * FIXME: It currently assumes there is only one managed profile.
 */
public class ManagedProfileSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private SwitchPreference mWorkModePreference;
    private RestrictedSwitchPreference mContactPrefrence;

    private UserManager mUserManager;
    private UserHandle mManagedUser;
    private Context mContext;

    private ManagedProfileBroadcastReceiver mManagedProfileBroadcastReceiver;

    private static final String KEY_WORK_MODE = "work_mode";
    private static final String KEY_CONTACT = "contacts_search";

    private static final String TAG = "ManagedProfileSettings";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.managed_profile_settings);
        mWorkModePreference = (SwitchPreference) findPreference(KEY_WORK_MODE);
        mWorkModePreference.setOnPreferenceChangeListener(this);
        mContactPrefrence = (RestrictedSwitchPreference) findPreference(KEY_CONTACT);
        mContactPrefrence.setOnPreferenceChangeListener(this);
        mContext = getActivity().getApplicationContext();
        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);
        mManagedUser = getManagedUserFromArgument();
        if (mManagedUser == null) {
            getActivity().finish();
        }
        mManagedProfileBroadcastReceiver = new ManagedProfileBroadcastReceiver();
        mManagedProfileBroadcastReceiver.register(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataAndPopulateUi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mManagedProfileBroadcastReceiver.unregister(getActivity());
    }

    private UserHandle getManagedUserFromArgument() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            UserHandle userHandle = arguments.getParcelable(Intent.EXTRA_USER);
            if (userHandle != null) {
                if (mUserManager.isManagedProfile(userHandle.getIdentifier())) {
                    return userHandle;
                }
            }
        }
        // Return default managed profile for the current user.
        return Utils.getManagedProfile(mUserManager);
    }

    private void loadDataAndPopulateUi() {
        if (mWorkModePreference != null) {
            mWorkModePreference.setChecked(
                    !mUserManager.isQuietModeEnabled(mManagedUser));
        }

        if (mContactPrefrence != null) {
            int value = Settings.Secure.getIntForUser(getContentResolver(),
                    MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 0, mManagedUser.getIdentifier());
            mContactPrefrence.setChecked(value != 0);
            RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                    RestrictedLockUtils.checkIfRemoteContactSearchDisallowed(
                            mContext, mManagedUser.getIdentifier());
            mContactPrefrence.setDisabledByAdmin(enforcedAdmin);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ACCOUNTS_WORK_PROFILE_SETTINGS;
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mWorkModePreference) {
            if ((boolean) newValue) {
                mUserManager.trySetQuietModeDisabled(mManagedUser.getIdentifier(), null);
            } else {
                mUserManager.setQuietModeEnabled(mManagedUser.getIdentifier(), true);
            }
            return true;
        }
        if (preference == mContactPrefrence) {
            int value = ((boolean) newValue == true) ? 1 : 0;
            Settings.Secure.putIntForUser(getContentResolver(),
                    MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, value, mManagedUser.getIdentifier());
            return true;
        }
        return false;
    }

    private class ManagedProfileBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.v(TAG, "Received broadcast: " + action);
            if (action.equals(Intent.ACTION_MANAGED_PROFILE_REMOVED)) {
                if (intent.getIntExtra(Intent.EXTRA_USER_HANDLE,
                        UserHandle.USER_NULL) == mManagedUser.getIdentifier()) {
                    getActivity().finish();
                }
                return;
            }

            if (action.equals(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                    || action.equals(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)) {
                if (intent.getIntExtra(Intent.EXTRA_USER_HANDLE,
                        UserHandle.USER_NULL) == mManagedUser.getIdentifier()) {
                    mWorkModePreference.setChecked(
                            !mUserManager.isQuietModeEnabled(mManagedUser));
                }
                return;
            }
            Log.w(TAG, "Cannot handle received broadcast: " + intent.getAction());
        }


        public void register(Context context) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
            intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
            intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
            context.registerReceiver(this, intentFilter);
        }

        public void unregister(Context context) {
            context.unregisterReceiver(this);
        }
    }

}
