/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import android.app.role.RoleManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;

/*
 * Abstract base controller for the default app shortcut preferences that launches the default app
 * settings with the corresponding default app highlighted.
 */
public abstract class DefaultAppShortcutPreferenceControllerBase extends BasePreferenceController {

    private final String mRoleName;

    protected final String mPackageName;

    private final RoleManager mRoleManager;

    public DefaultAppShortcutPreferenceControllerBase(Context context, String preferenceKey,
            String roleName, String packageName) {
        super(context, preferenceKey);

        mRoleName = roleName;
        mPackageName = packageName;

        mRoleManager = context.getSystemService(RoleManager.class);
    }

    // TODO: STOPSHIP(b/110557011): Remove this once we have all default apps migrated.
    public DefaultAppShortcutPreferenceControllerBase(Context context, String preferenceKey,
            String packageName) {
        this(context, preferenceKey, null /* roleName */, packageName);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getSystemService(UserManager.class).isManagedProfile()) {
            return DISABLED_FOR_USER;
        }
        return hasAppCapability() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        final int summaryResId = isDefaultApp() ? R.string.yes : R.string.no;
        return mContext.getText(summaryResId);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(mPreferenceKey, preference.getKey())) {
            return false;
        }
        // TODO: STOPSHIP(b/110557011): Remove this check once we have all default apps migrated.
        if (mRoleName != null) {
            final Intent intent = new Intent(Intent.ACTION_MANAGE_DEFAULT_APP)
                    .putExtra(Intent.EXTRA_ROLE_NAME, mRoleName);
            mContext.startActivity(intent);
        } else {
            final Bundle bundle = new Bundle();
            bundle.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, mPreferenceKey);
            new SubSettingLauncher(mContext)
                    .setDestination(DefaultAppSettings.class.getName())
                    .setArguments(bundle)
                    .setTitleRes(R.string.configure_apps)
                    .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                    .launch();
        }
        return true;
    }

    /**
     * Check whether the app has the default app capability
     *
     * @return true if the app has the default app capability
     */
    protected boolean hasAppCapability() {
        // TODO: STOPSHIP(b/110557011): Remove this check once we have all default apps migrated.
        if (mRoleName != null) {
            return mRoleManager.isRoleAvailable(mRoleName);
        }
        return false;
    }

    /**
     * Check whether the app is the default app
     *
     * @return true if the app is the default app
     */
    protected boolean isDefaultApp() {
        // TODO: STOPSHIP(b/110557011): Remove this check once we have all default apps migrated.
        if (mRoleName != null) {
            final String packageName = CollectionUtils.firstOrNull(mRoleManager.getRoleHolders(
                    mRoleName));
            return TextUtils.equals(mPackageName, packageName);
        }
        return false;
    }
}
