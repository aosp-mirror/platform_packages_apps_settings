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

package com.android.settings.development;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class LinuxTerminalPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private static final String TAG = "LinuxTerminalPrefCtrl";

    private static final String ENABLE_TERMINAL_KEY = "enable_linux_terminal";

    @NonNull
    private final PackageManager mPackageManager;

    @Nullable
    private final String mTerminalPackageName;

    public LinuxTerminalPreferenceController(@NonNull Context context) {
        super(context);
        mPackageManager = mContext.getPackageManager();

        String packageName = mContext.getString(R.string.config_linux_terminal_app_package_name);
        mTerminalPackageName =
                isPackageInstalled(mPackageManager, packageName) ? packageName : null;

        Log.d(TAG, "Terminal app package name=" + packageName + ", isAvailable=" + isAvailable());
    }

    // Avoid lazy initialization because this may be called before displayPreference().
    @Override
    public boolean isAvailable() {
        // Returns true only if the terminal app is installed which only happens when the build flag
        // RELEASE_AVF_SUPPORT_CUSTOM_VM_WITH_PARAVIRTUALIZED_DEVICES is true.
        // TODO(b/343795511): Add explicitly check for the flag when it's accessible from Java code.
        return getTerminalPackageName() != null;
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return ENABLE_TERMINAL_KEY;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference.setEnabled(isAvailable());
    }

    @Override
    public boolean onPreferenceChange(
                @NonNull Preference preference, @NonNull Object newValue) {
        String packageName = getTerminalPackageName();
        if (packageName == null) {
            return false;
        }

        boolean terminalEnabled = (Boolean) newValue;
        int state = terminalEnabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        mPackageManager.setApplicationEnabledSetting(packageName, state, /* flags=*/ 0);
        ((TwoStatePreference) mPreference).setChecked(terminalEnabled);
        return true;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        String packageName = getTerminalPackageName();
        if (packageName == null) {
            return;
        }

        boolean isTerminalEnabled = mPackageManager.getApplicationEnabledSetting(packageName)
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        ((TwoStatePreference) mPreference).setChecked(isTerminalEnabled);
    }

    // Can be mocked for testing
    @VisibleForTesting
    @Nullable
    String getTerminalPackageName() {
        return mTerminalPackageName;
    }

    private static boolean isPackageInstalled(PackageManager manager, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            return manager.getPackageInfo(
                    packageName,
                    PackageManager.MATCH_ALL | PackageManager.MATCH_DISABLED_COMPONENTS) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
