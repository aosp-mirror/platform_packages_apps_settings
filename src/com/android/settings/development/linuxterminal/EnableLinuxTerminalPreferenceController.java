/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.development.linuxterminal;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/** Preference controller for enable/disable toggle of the linux terminal */
public class EnableLinuxTerminalPreferenceController extends BasePreferenceController
        implements CompoundButton.OnCheckedChangeListener, PreferenceControllerMixin {
    @VisibleForTesting
    static final int TERMINAL_PACKAGE_NAME_RESID = R.string.config_linux_terminal_app_package_name;

    private static final String TAG = "LinuxTerminalPrefCtrl";

    private static final String ENABLE_TERMINAL_KEY = "enable_linux_terminal";

    @NonNull private final PackageManager mPackageManager;
    private final boolean mIsPrimaryUser;
    @Nullable private final String mTerminalPackageName;

    @Nullable private SettingsMainSwitchPreference mPreference;

    public EnableLinuxTerminalPreferenceController(
            @NonNull Context context, @NonNull Context userAwareContext, int userId) {
        this(context, userAwareContext, userId == UserHandle.myUserId());
    }

    @VisibleForTesting
    EnableLinuxTerminalPreferenceController(
            @NonNull Context context, @NonNull Context userAwareContext, boolean isPrimaryUser) {
        super(context, ENABLE_TERMINAL_KEY);

        mPackageManager = userAwareContext.getPackageManager();
        mIsPrimaryUser = isPrimaryUser;

        String packageName =
                userAwareContext.getString(R.string.config_linux_terminal_app_package_name);
        mTerminalPackageName =
                isPackageInstalled(mPackageManager, packageName) ? packageName : null;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.addOnSwitchChangeListener(this);
        }
    }

    @Override
    public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
        if (mTerminalPackageName == null) {
            return;
        }

        int state =
                isChecked
                        ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        mPackageManager.setApplicationEnabledSetting(mTerminalPackageName, state, /* flags= */ 0);
        if (!isChecked) {
            mPackageManager.clearApplicationUserData(
                    mTerminalPackageName, /* observer= */ null);
        }
    }

    @Override
    @SuppressWarnings("NullAway") // setDisabledByAdmin(EnforcedAdmin) doesn't have @Nullable
    public void updateState(@NonNull Preference preference) {
        if (mPreference != preference) {
            return;
        }

        boolean isInstalled = (mTerminalPackageName != null);
        if (isInstalled) {
            mPreference.setDisabledByAdmin(/* admin= */ null);
            mPreference.setEnabled(/* enabled= */ true);
            boolean terminalEnabled =
                    mPackageManager.getApplicationEnabledSetting(mTerminalPackageName)
                            == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            mPreference.setChecked(terminalEnabled);
        } else {
            if (mIsPrimaryUser) {
                Log.e(TAG, "Terminal app doesn't exist for primary user but UI was shown");
                mPreference.setDisabledByAdmin(/* admin= */ null);
                mPreference.setEnabled(/* enabled= */ false);
            } else {
                // If admin hasn't enabled the system app, mark it as disabled by admin.
                mPreference.setDisabledByAdmin(new EnforcedAdmin());
                // Make it enabled, so clicking it would show error dialog.
                mPreference.setEnabled(/* enabled= */ true);
            }
            mPreference.setChecked(/* checked= */ false);
        }
    }

    private static boolean isPackageInstalled(PackageManager manager, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            return manager.getPackageInfo(
                            packageName,
                            PackageManager.MATCH_ALL | PackageManager.MATCH_DISABLED_COMPONENTS)
                    != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
