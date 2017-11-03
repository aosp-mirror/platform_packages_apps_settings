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

package com.android.settings.security;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.TimeoutListPreference;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ManagedLockPasswordProvider;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settingslib.RestrictedLockUtils;

import java.util.ArrayList;

public class SecuritySubSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener,
        OwnerInfoPreferenceController.OwnerInfoCallback {

    private static final String TAG = "SecuritySubSettings";

    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";

    // These switch preferences need special handling since they're not all stored in Settings.
    private static final String SWITCH_PREFERENCE_KEYS[] = {
            KEY_LOCK_AFTER_TIMEOUT, KEY_VISIBLE_PATTERN, KEY_POWER_INSTANTLY_LOCKS};
    private static final int MY_USER_ID = UserHandle.myUserId();

    private TimeoutListPreference mLockAfter;
    private SwitchPreference mVisiblePattern;
    private SwitchPreference mPowerButtonInstantlyLocks;

    private TrustAgentManager mTrustAgentManager;
    private LockPatternUtils mLockPatternUtils;
    private DevicePolicyManager mDPM;
    private OwnerInfoPreferenceController mOwnerInfoPreferenceController;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SECURITY;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SecurityFeatureProvider securityFeatureProvider =
                FeatureFactory.getFactory(getActivity()).getSecurityFeatureProvider();
        mTrustAgentManager = securityFeatureProvider.getTrustAgentManager();
        mLockPatternUtils = new LockPatternUtils(getContext());
        mDPM = getContext().getSystemService(DevicePolicyManager.class);
        mOwnerInfoPreferenceController =
                new OwnerInfoPreferenceController(getContext(), this, null /* lifecycle */);
        createPreferenceHierarchy();
    }

    @Override
    public void onResume() {
        super.onResume();

        createPreferenceHierarchy();

        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(mLockPatternUtils.isVisiblePatternEnabled(MY_USER_ID));
        }
        if (mPowerButtonInstantlyLocks != null) {
            mPowerButtonInstantlyLocks.setChecked(
                    mLockPatternUtils.getPowerButtonInstantlyLocks(MY_USER_ID));
        }

        mOwnerInfoPreferenceController.updateSummary();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        createPreferenceHierarchy();
    }

    private void createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }

        final int resid = getResIdForLockUnlockSubScreen(new LockPatternUtils(getContext()),
                ManagedLockPasswordProvider.get(getContext(), MY_USER_ID));
        addPreferencesFromResource(resid);

        // lock after preference
        mLockAfter = (TimeoutListPreference) findPreference(KEY_LOCK_AFTER_TIMEOUT);
        if (mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        }

        // visible pattern
        mVisiblePattern = (SwitchPreference) findPreference(KEY_VISIBLE_PATTERN);

        // lock instantly on power key press
        mPowerButtonInstantlyLocks = (SwitchPreference) findPreference(
                KEY_POWER_INSTANTLY_LOCKS);
        CharSequence trustAgentLabel = getActiveTrustAgentLabel(getContext(),
                mTrustAgentManager, mLockPatternUtils, mDPM);
        if (mPowerButtonInstantlyLocks != null && !TextUtils.isEmpty(trustAgentLabel)) {
            mPowerButtonInstantlyLocks.setSummary(getString(
                    R.string.lockpattern_settings_power_button_instantly_locks_summary,
                    trustAgentLabel));
        }

        mOwnerInfoPreferenceController.displayPreference(getPreferenceScreen());
        mOwnerInfoPreferenceController.updateEnableState();

        for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
            final Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
            if (pref != null) pref.setOnPreferenceChangeListener(this);
        }
    }

    private void setupLockAfterPreference() {
        // Compatible with pre-Froyo
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        mLockAfter.setValue(String.valueOf(currentTimeout));
        mLockAfter.setOnPreferenceChangeListener(this);
        if (mDPM != null) {
            final RestrictedLockUtils.EnforcedAdmin admin =
                    RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(
                            getActivity());
            final long adminTimeout = mDPM
                    .getMaximumTimeToLockForUserAndProfiles(UserHandle.myUserId());
            final long displayTimeout = Math.max(0,
                    Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
            // This setting is a slave to display timeout when a device policy is enforced.
            // As such, maxLockTimeout = adminTimeout - displayTimeout.
            // If there isn't enough time, shows "immediately" setting.
            final long maxTimeout = Math.max(0, adminTimeout - displayTimeout);
            mLockAfter.removeUnusableTimeouts(maxTimeout, admin);
        }
    }

    private void updateLockAfterPreferenceSummary() {
        final String summary;
        if (mLockAfter.isDisabledByAdmin()) {
            summary = getString(R.string.disabled_by_policy_title);
        } else {
            // Update summary message with current value
            long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
            final CharSequence[] entries = mLockAfter.getEntries();
            final CharSequence[] values = mLockAfter.getEntryValues();
            int best = 0;
            for (int i = 0; i < values.length; i++) {
                long timeout = Long.valueOf(values[i].toString());
                if (currentTimeout >= timeout) {
                    best = i;
                }
            }

            CharSequence trustAgentLabel = getActiveTrustAgentLabel(getContext(),
                    mTrustAgentManager, mLockPatternUtils, mDPM);
            if (!TextUtils.isEmpty(trustAgentLabel)) {
                if (Long.valueOf(values[best].toString()) == 0) {
                    summary = getString(R.string.lock_immediately_summary_with_exception,
                            trustAgentLabel);
                } else {
                    summary = getString(R.string.lock_after_timeout_summary_with_exception,
                            entries[best], trustAgentLabel);
                }
            } else {
                summary = getString(R.string.lock_after_timeout_summary, entries[best]);
            }
        }
        mLockAfter.setSummary(summary);
    }

    @Override
    public void onOwnerInfoUpdated() {
        mOwnerInfoPreferenceController.updateSummary();
    }

    static int getResIdForLockUnlockSubScreen(LockPatternUtils lockPatternUtils,
            ManagedLockPasswordProvider managedPasswordProvider) {
        if (lockPatternUtils.isSecure(MY_USER_ID)) {
            switch (lockPatternUtils.getKeyguardStoredPasswordQuality(MY_USER_ID)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return R.xml.security_settings_pattern_sub;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return R.xml.security_settings_pin_sub;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    return R.xml.security_settings_password_sub;
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    return managedPasswordProvider.getResIdForLockUnlockSubScreen();
            }
        } else if (!lockPatternUtils.isLockScreenDisabled(MY_USER_ID)) {
            return R.xml.security_settings_slide_sub;
        }
        return 0;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String key = preference.getKey();
        if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
            mLockPatternUtils.setPowerButtonInstantlyLocks((Boolean) value, MY_USER_ID);
        } else if (KEY_LOCK_AFTER_TIMEOUT.equals(key)) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist lockAfter timeout setting", e);
            }
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            mLockPatternUtils.setVisiblePatternEnabled((Boolean) value, MY_USER_ID);
        }
        return true;
    }

    private static CharSequence getActiveTrustAgentLabel(Context context,
            TrustAgentManager trustAgentManager, LockPatternUtils utils,
            DevicePolicyManager dpm) {
        ArrayList<TrustAgentManager.TrustAgentComponentInfo> agents =
                SecuritySettings.getActiveTrustAgents(context, trustAgentManager, utils, dpm);
        return agents.isEmpty() ? null : agents.get(0).title;
    }
}
