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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.List;

/**
 * Controller to manage the state of "Verify apps over USB" toggle.
 */
public class VerifyAppsOverUsbPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, AdbOnChangeListener,
        PreferenceControllerMixin {
    private static final String VERIFY_APPS_OVER_USB_KEY = "verify_apps_over_usb";
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";

    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    /**
     * Class for indirection of RestrictedLockUtils for testing purposes. It would be nice to mock
     * the appropriate methods in UserManager instead but they aren't accessible.
     */
    @VisibleForTesting
    class RestrictedLockUtilsDelegate {
        public EnforcedAdmin checkIfRestrictionEnforced(
                Context context, String userRestriction, int userId) {
            return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(context, userRestriction,
                    userId);
        }
    }

    // NB: This field is accessed using reflection in the test, please keep name in sync.
    private final RestrictedLockUtilsDelegate mRestrictedLockUtils =
            new RestrictedLockUtilsDelegate();

    // This field is accessed using reflection in the test, please keep name in sync.
    private final PackageManager mPackageManager;

    public VerifyAppsOverUsbPreferenceController(Context context) {
        super(context);

        mPackageManager = context.getPackageManager();
    }

    @Override
    public boolean isAvailable() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 1 /* default */) > 0;
    }

    @Override
    public String getPreferenceKey() {
        return VERIFY_APPS_OVER_USB_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final RestrictedSwitchPreference restrictedPreference =
            (RestrictedSwitchPreference) preference;
        if (!shouldBeEnabled()) {
            restrictedPreference.setChecked(false);
            restrictedPreference.setDisabledByAdmin(null);
            restrictedPreference.setEnabled(false);
            return;
        }

        final EnforcedAdmin enforcingAdmin = mRestrictedLockUtils.checkIfRestrictionEnforced(
                mContext, UserManager.ENSURE_VERIFY_APPS, UserHandle.myUserId());
        if (enforcingAdmin != null) {
            restrictedPreference.setChecked(true);
            restrictedPreference.setDisabledByAdmin(enforcingAdmin);
            return;
        }

        restrictedPreference.setEnabled(true);
        final boolean checked = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, SETTING_VALUE_ON)
                != SETTING_VALUE_OFF;
        restrictedPreference.setChecked(checked);
    }

    @Override
    public void onAdbSettingChanged() {
        if (isAvailable()) {
            updateState(mPreference);
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        super.onDeveloperOptionsSwitchEnabled();
        updateState(mPreference);
    }

    /**
     * Checks whether the toggle should be enabled depending on whether verify apps over USB is
     * possible currently. If ADB is disabled or if package verifier does not exist, the toggle
     * should be disabled.
     */
    private boolean shouldBeEnabled() {
        final ContentResolver cr = mContext.getContentResolver();
        if (Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED,
                AdbPreferenceController.ADB_SETTING_OFF)
                == AdbPreferenceController.ADB_SETTING_OFF) {
            return false;
        }
        final Intent verification = new Intent(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION);
        verification.setType(PACKAGE_MIME_TYPE);
        verification.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        final List<ResolveInfo> receivers = mPackageManager.queryBroadcastReceivers(
                verification, 0 /* flags */);
        return !receivers.isEmpty();
    }
}
