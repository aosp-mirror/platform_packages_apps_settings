/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;

import java.util.Arrays;
import java.util.List;

/**
 * Screen pinning settings.
 */
public class ScreenPinningSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, Indexable {

    private static final CharSequence KEY_USE_SCREEN_LOCK = "use_screen_lock";
    private static final int CHANGE_LOCK_METHOD_REQUEST = 43;

    private SwitchBar mSwitchBar;
    private SwitchPreference mUseScreenLock;
    private LockPatternUtils mLockPatternUtils;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SCREEN_PINNING;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        activity.setTitle(R.string.screen_pinning_title);
        mLockPatternUtils = new LockPatternUtils(activity);


        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();
        mSwitchBar.setChecked(isLockToAppEnabled(getActivity()));
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_screen_pinning;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup parent = (ViewGroup) view.findViewById(android.R.id.list_container);
        View emptyView = LayoutInflater.from(getContext())
                .inflate(R.layout.screen_pinning_instructions, parent, false);
        parent.addView(emptyView);
        setEmptyView(emptyView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    private static boolean isLockToAppEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCK_TO_APP_ENABLED, 0) != 0;
    }

    private void setLockToAppEnabled(boolean isEnabled) {
        Settings.System.putInt(getContentResolver(), Settings.System.LOCK_TO_APP_ENABLED,
                isEnabled ? 1 : 0);
        if (isEnabled) {
            // Set the value to match what we have defaulted to in the UI.
            setScreenLockUsedSetting(isScreenLockUsed());
        }
    }

    private boolean isScreenLockUsed() {
        // This functionality should be kept consistent with
        // com.android.server.wm.LockTaskController (see b/127605586)
        int defaultValueIfSettingNull = mLockPatternUtils.isSecure(UserHandle.myUserId()) ? 1 : 0;
        return Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.LOCK_TO_APP_EXIT_LOCKED,
                defaultValueIfSettingNull) != 0;
    }

    private boolean setScreenLockUsed(boolean isEnabled) {
        if (isEnabled) {
            LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
            int passwordQuality = lockPatternUtils
                    .getKeyguardStoredPasswordQuality(UserHandle.myUserId());
            if (passwordQuality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                Intent chooseLockIntent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                chooseLockIntent.putExtra(
                        ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                startActivityForResult(chooseLockIntent, CHANGE_LOCK_METHOD_REQUEST);
                return false;
            }
        }
        setScreenLockUsedSetting(isEnabled);
        return true;
    }

    private void setScreenLockUsedSetting(boolean isEnabled) {
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCK_TO_APP_EXIT_LOCKED,
                isEnabled ? 1 : 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHANGE_LOCK_METHOD_REQUEST) {
            LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
            boolean validPassQuality = lockPatternUtils.getKeyguardStoredPasswordQuality(
                    UserHandle.myUserId())
                    != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
            setScreenLockUsed(validPassQuality);
            // Make sure the screen updates.
            mUseScreenLock.setChecked(validPassQuality);
        }
    }

    private int getCurrentSecurityTitle() {
        int quality = mLockPatternUtils.getKeyguardStoredPasswordQuality(
                UserHandle.myUserId());
        switch (quality) {
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                return R.string.screen_pinning_unlock_pin;
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                return R.string.screen_pinning_unlock_password;
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                if (mLockPatternUtils.isLockPatternEnabled(UserHandle.myUserId())) {
                    return R.string.screen_pinning_unlock_pattern;
                }
        }
        return R.string.screen_pinning_unlock_none;
    }

    /**
     * Listens to the state change of the lock-to-app master switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setLockToAppEnabled(isChecked);
        updateDisplay();
    }

    public void updateDisplay() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        if (isLockToAppEnabled(getActivity())) {
            addPreferencesFromResource(R.xml.screen_pinning_settings);
            root = getPreferenceScreen();

            mUseScreenLock = (SwitchPreference) root.findPreference(KEY_USE_SCREEN_LOCK);
            mUseScreenLock.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return setScreenLockUsed((boolean) newValue);
                }
            });
            mUseScreenLock.setChecked(isScreenLockUsed());
            mUseScreenLock.setTitle(getCurrentSecurityTitle());
        }
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                    boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.screen_pinning_settings;
                return Arrays.asList(sir);
            }
        };
}
