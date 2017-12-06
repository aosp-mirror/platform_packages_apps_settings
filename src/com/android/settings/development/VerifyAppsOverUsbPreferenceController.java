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
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

/**
 * Controller to manage the state of "Verify apps over USB" toggle.
 */
public class VerifyAppsOverUsbPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {
    private static final String VERIFY_APPS_OVER_USB_KEY = "verify_apps_over_usb";
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";

    private RestrictedSwitchPreference mPreference;

    /**
     * Class for indirection of RestrictedLockUtils for testing purposes. It would be nice to mock
     * the appropriate methods in UserManager instead but they aren't accessible.
     */
    @VisibleForTesting
    class RestrictedLockUtilsDelegate {
        public EnforcedAdmin checkIfRestrictionEnforced(
                Context context, String userRestriction, int userId) {
            return RestrictedLockUtils.checkIfRestrictionEnforced(context, userRestriction, userId);
        }
    }
    // NB: This field is accessed using reflection in the test, please keep name in sync.
    private final RestrictedLockUtilsDelegate mRestrictedLockUtils =
            new RestrictedLockUtilsDelegate();

    VerifyAppsOverUsbPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = (RestrictedSwitchPreference)
                    screen.findPreference(VERIFY_APPS_OVER_USB_KEY);
        }
    }

    @Override
    public boolean isAvailable() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 1) > 0;
    }

    @Override
    public String getPreferenceKey() {
        return VERIFY_APPS_OVER_USB_KEY;
    }

    /** Saves the settings value when it is toggled. */
    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (VERIFY_APPS_OVER_USB_KEY.equals(preference.getKey())) {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, mPreference.isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }

    /**
     * Checks whether the toggle should be enabled depending on whether verify apps over USB is
     * possible currently. If ADB is disabled or if package verifier does not exist, the toggle
     * should be disabled.
     */
    private boolean shouldBeEnabled() {
        final ContentResolver cr = mContext.getContentResolver();
        if (Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 0) {
            return false;
        }
        if (Settings.Global.getInt(cr, Settings.Global.PACKAGE_VERIFIER_ENABLE, 1) == 0) {
            return false;
        } else {
            final PackageManager pm = mContext.getPackageManager();
            final Intent verification = new Intent(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION);
            verification.setType(PACKAGE_MIME_TYPE);
            verification.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(verification, 0);
            if (receivers.size() == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Updates position, enabled status and maybe admin message.
     */
    public void updatePreference() {
        if (!isAvailable()) {
            return;
        }

        if (!shouldBeEnabled()) {
            mPreference.setChecked(false);
            mPreference.setDisabledByAdmin(null);
            mPreference.setEnabled(false);
            return;
        }

        final EnforcedAdmin enforcingAdmin = mRestrictedLockUtils.checkIfRestrictionEnforced(
                        mContext, UserManager.ENSURE_VERIFY_APPS, UserHandle.myUserId());
        if (enforcingAdmin != null) {
            mPreference.setChecked(true);
            mPreference.setDisabledByAdmin(enforcingAdmin);
            return;
        }

        mPreference.setEnabled(true);
        final boolean checked = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1) != 0;
        mPreference.setChecked(checked);
    }
}
