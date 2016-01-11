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
package com.android.settings;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings for the Profile Challenge.
 */
public class ProfileChallengePreferenceFragment extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {
    private static final String TAG = "WorkChallengePreferenceFragment";

    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;

    // Not all preferences make sense for the Work Challenge, this is a whitelist.
    private static final Set<String> ALLOWED_PREFERENCE_KEYS = new HashSet<>();
    {
        ALLOWED_PREFERENCE_KEYS.add(KEY_UNLOCK_SET_OR_CHANGE);
        ALLOWED_PREFERENCE_KEYS.add(KEY_VISIBLE_PATTERN);
    }
    // These switch preferences need special handling since they're not all stored in Settings.
    private static final Set<String> SWITCH_PREFERENCE_KEYS = new HashSet<>();
    {
        SWITCH_PREFERENCE_KEYS.add(KEY_VISIBLE_PATTERN);
    }

    private LockPatternUtils mLockPatternUtils;
    private int mProfileUserId;

    private SwitchPreference mVisiblePattern;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.PROFILE_CHALLENGE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mProfileUserId = getArguments().getInt(Intent.EXTRA_USER_ID, -1);
        if (mProfileUserId == -1) {
            finish();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            Bundle extras = new Bundle();
            extras.putInt(Intent.EXTRA_USER_ID, mProfileUserId);
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, extras);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (KEY_VISIBLE_PATTERN.equals(key)) {
            mLockPatternUtils.setVisiblePatternEnabled((Boolean) value, mProfileUserId);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(mLockPatternUtils.isVisiblePatternEnabled(
                    mProfileUserId));
        }
    }

    private void createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.profile_challenge_settings);
        root = getPreferenceScreen();

        // Add options for lock/unlock screen
        final int resid = getResIdForLockUnlockScreen(getActivity(), mLockPatternUtils);
        addPreferencesFromResource(resid);

        mVisiblePattern = (SwitchPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        removeNonWhitelistedItems(root);
    }

    private void removeNonWhitelistedItems(PreferenceGroup prefScreen) {
        int numPreferences = prefScreen.getPreferenceCount();
        int i = 0;
        while (i < numPreferences) {
            final Preference pref = prefScreen.getPreference(i);
            // Recursively look into categories and remove them if they are empty.
            if (pref instanceof PreferenceCategory) {
                PreferenceCategory category = (PreferenceCategory) pref;
                removeNonWhitelistedItems(category);
                if (category.getPreferenceCount() == 0) {
                    prefScreen.removePreference(category);
                    --i;
                    --numPreferences;
                }
            } else if (ALLOWED_PREFERENCE_KEYS.contains(pref.getKey())) {
                if (SWITCH_PREFERENCE_KEYS.contains(pref.getKey())) {
                    pref.setOnPreferenceChangeListener(this);
                }
            } else {
                prefScreen.removePreference(pref);
                --i;
                --numPreferences;
            }
            ++i;
        }
    }

    private int getResIdForLockUnlockScreen(Context context,
            LockPatternUtils lockPatternUtils) {
        int resid = 0;
        if (!lockPatternUtils.isSecure(mProfileUserId)) {
            resid = R.xml.security_settings_lockscreen;
        } else {
            switch (lockPatternUtils.getKeyguardStoredPasswordQuality(mProfileUserId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }
        return resid;
    }
}
