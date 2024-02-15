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

import static android.app.Activity.RESULT_OK;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.icu.text.MessageFormat;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.FooterPreference;

import java.util.List;

/**
 * Screen pinning settings.
 */
@SearchIndexable
public class ScreenPinningSettings extends SettingsPreferenceFragment
        implements OnCheckedChangeListener, DialogInterface.OnClickListener {

    private static final String KEY_USE_SCREEN_LOCK = "use_screen_lock";
    private static final String KEY_FOOTER = "screen_pinning_settings_screen_footer";
    private static final int CHANGE_LOCK_METHOD_REQUEST = 43;
    private static final int CONFIRM_REQUEST = 1000;

    private SettingsMainSwitchBar mSwitchBar;
    private TwoStatePreference mUseScreenLock;
    private FooterPreference mFooterPreference;
    private LockPatternUtils mLockPatternUtils;
    private UserManager mUserManager;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SCREEN_PINNING;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        activity.setTitle(R.string.screen_pinning_title);
        mLockPatternUtils = new LockPatternUtils(activity);
        mUserManager = activity.getSystemService(UserManager.class);

        addPreferencesFromResource(R.xml.screen_pinning_settings);
        final PreferenceScreen root = getPreferenceScreen();
        mUseScreenLock = root.findPreference(KEY_USE_SCREEN_LOCK);
        mFooterPreference = root.findPreference(KEY_FOOTER);

        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.setTitle(getContext().getString(R.string.app_pinning_main_switch_title));
        mSwitchBar.show();
        mSwitchBar.setChecked(isLockToAppEnabled(getActivity()));
        mSwitchBar.addOnSwitchChangeListener(this);

        updateDisplay();
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_screen_pinning;
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
        LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
        final int passwordQuality = lockPatternUtils
                .getKeyguardStoredPasswordQuality(UserHandle.myUserId());
        if (isEnabled) {
            if (passwordQuality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                Intent chooseLockIntent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                chooseLockIntent.putExtra(
                        ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS,
                        true);
                startActivityForResult(chooseLockIntent, CHANGE_LOCK_METHOD_REQUEST);
                return false;
            }
        }  else {
            if (passwordQuality != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                final ChooseLockSettingsHelper.Builder builder =
                        new ChooseLockSettingsHelper.Builder(getActivity(), this);
                return builder.setRequestCode(CONFIRM_REQUEST).show();
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
        } else if (requestCode == CONFIRM_REQUEST && resultCode == RESULT_OK) {
            setScreenLockUsedSetting(false);
        }
    }

    private static int getCurrentSecurityTitle(LockPatternUtils lockPatternUtils) {
        int quality = lockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId());
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
                if (lockPatternUtils.isLockPatternEnabled(UserHandle.myUserId())) {
                    return R.string.screen_pinning_unlock_pattern;
                }
        }
        return R.string.screen_pinning_unlock_none;
    }

    /**
     * Listens to the state change of the overall lock-to-app switch.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.screen_pinning_dialog_message)
                    .setPositiveButton(R.string.dlg_ok, this)
                    .setNegativeButton(R.string.dlg_cancel, this)
                    .setCancelable(false)
                    .show();
        } else {
            setLockToAppEnabled(false);
            updateDisplay();
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            setLockToAppEnabled(true);
        } else {
            mSwitchBar.setChecked(false);
        }
        updateDisplay();
    }

    private void updateDisplay() {
        if (isLockToAppEnabled(getActivity())) {
            mUseScreenLock.setEnabled(true);
            mUseScreenLock.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return setScreenLockUsed((boolean) newValue);
                }
            });
            mUseScreenLock.setChecked(isScreenLockUsed());
            mUseScreenLock.setTitle(getCurrentSecurityTitle(mLockPatternUtils));
        } else {
            mFooterPreference.setSummary(getAppPinningContent());
            mUseScreenLock.setEnabled(false);
        }
    }

    private boolean isGuestModeSupported() {
        return UserManager.supportsMultipleUsers()
                && !mUserManager.hasUserRestriction(UserManager.DISALLOW_USER_SWITCH);
    }

    private CharSequence getAppPinningContent() {
        final int stringResource = isGuestModeSupported()
                ? R.string.screen_pinning_guest_user_description
                : R.string.screen_pinning_description;
        return MessageFormat.format(getActivity().getString(stringResource), 1, 2, 3);
    }

    /**
     * For search.
     *
     * This page only provides an index for the toggle preference of using screen lock for
     * unpinning. The preference name will change with various lock configurations. Indexing data
     * from XML isn't suitable since it uses a static title by default. So, we skip XML indexing
     * by omitting the XML argument in the constructor and use a dynamic index method instead.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @NonNull
                @Override
                public List<SearchIndexableRaw> getDynamicRawDataToIndex(@NonNull Context context,
                        boolean enabled) {
                    List<SearchIndexableRaw> dynamicRaws =
                            super.getDynamicRawDataToIndex(context, enabled);
                    final SearchIndexableRaw raw = new SearchIndexableRaw(context);
                    final Resources res = context.getResources();
                    final LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
                    raw.key = KEY_USE_SCREEN_LOCK;
                    raw.title = res.getString(getCurrentSecurityTitle(lockPatternUtils));
                    raw.screenTitle = res.getString(R.string.screen_pinning_title);
                    dynamicRaws.add(raw);
                    return dynamicRaws;
                }
            };
}
