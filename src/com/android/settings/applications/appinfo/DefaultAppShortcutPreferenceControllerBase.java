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

import android.app.role.RoleControllerManager;
import android.app.role.RoleManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.concurrent.Executor;

/*
 * Abstract base controller for the default app shortcut preferences that launches the default app
 * settings with the corresponding default app highlighted.
 */
public abstract class DefaultAppShortcutPreferenceControllerBase extends BasePreferenceController {

    private final String mRoleName;

    protected final String mPackageName;

    private final RoleManager mRoleManager;

    private boolean mRoleVisible;

    private boolean mAppVisible;

    private PreferenceScreen mPreferenceScreen;

    public DefaultAppShortcutPreferenceControllerBase(Context context, String preferenceKey,
            String roleName, String packageName) {
        super(context, preferenceKey);

        mRoleName = roleName;
        mPackageName = packageName;

        mRoleManager = context.getSystemService(RoleManager.class);

        final RoleControllerManager roleControllerManager =
                mContext.getSystemService(RoleControllerManager.class);
        final Executor executor = mContext.getMainExecutor();
        roleControllerManager.isRoleVisible(mRoleName, executor, visible -> {
            mRoleVisible = visible;
            refreshAvailability();
        });
        roleControllerManager.isApplicationVisibleForRole(mRoleName, mPackageName, executor,
                visible -> {
                    mAppVisible = visible;
                    refreshAvailability();
                });
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceScreen = screen;
    }

    private void refreshAvailability() {
        if (mPreferenceScreen != null) {
            final Preference preference = mPreferenceScreen.findPreference(getPreferenceKey());
            if (preference != null) {
                preference.setVisible(isAvailable());
                updateState(preference);
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getSystemService(UserManager.class).isManagedProfile()) {
            return DISABLED_FOR_USER;
        }
        return mRoleVisible && mAppVisible ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
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
        final Intent intent = new Intent(Intent.ACTION_MANAGE_DEFAULT_APP)
                .putExtra(Intent.EXTRA_ROLE_NAME, mRoleName);
        mContext.startActivity(intent);
        return true;
    }

    /**
     * Check whether the app is the default app
     *
     * @return true if the app is the default app
     */
    private boolean isDefaultApp() {
        final String packageName = CollectionUtils.firstOrNull(mRoleManager.getRoleHolders(
                mRoleName));
        return TextUtils.equals(mPackageName, packageName);
    }
}
