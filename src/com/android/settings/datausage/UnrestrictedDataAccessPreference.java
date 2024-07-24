/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.datausage;

import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfMeteredDataUsageUserControlDisabled;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.AppSwitchPreference;

public class UnrestrictedDataAccessPreference extends AppSwitchPreference implements
        DataSaverBackend.Listener {
    private static final String ECM_SETTING_IDENTIFIER = "android:unrestricted_data_access";

    private final ApplicationsState mApplicationsState;
    private final AppEntry mEntry;
    private final AppStateDataUsageBridge.DataUsageState mDataUsageState;
    private final DataSaverBackend mDataSaverBackend;
    private final DashboardFragment mParentFragment;
    private final RestrictedPreferenceHelper mHelper;
    private Drawable mCacheIcon;

    public UnrestrictedDataAccessPreference(final Context context, AppEntry entry,
            ApplicationsState applicationsState, DataSaverBackend dataSaverBackend,
            DashboardFragment parentFragment) {
        super(context);
        mHelper = new RestrictedPreferenceHelper(context, this, null);
        mEntry = entry;
        mDataUsageState = (AppStateDataUsageBridge.DataUsageState) mEntry.extraInfo;
        mEntry.ensureLabel(context);
        mApplicationsState = applicationsState;
        mDataSaverBackend = dataSaverBackend;
        mParentFragment = parentFragment;
        setDisabledByAdmin(checkIfMeteredDataUsageUserControlDisabled(
                context, entry.info.packageName, UserHandle.getUserId(entry.info.uid)));
        mHelper.checkEcmRestrictionAndSetDisabled(ECM_SETTING_IDENTIFIER, entry.info.packageName);
        updateState();
        setKey(generateKey(mEntry));

        mCacheIcon = AppUtils.getIconFromCache(mEntry);
        if (mCacheIcon != null) {
            setIcon(mCacheIcon);
        } else {
            // Set empty icon as default.
            setIcon(R.drawable.empty_icon);
        }
    }

    static String generateKey(final AppEntry entry) {
        return entry.info.packageName + "|" + entry.info.uid;
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mDataSaverBackend.addListener(this);
    }

    @Override
    public void onDetached() {
        mDataSaverBackend.remListener(this);
        super.onDetached();
    }

    @Override
    protected void onClick() {
        if (mDataUsageState != null && mDataUsageState.isDataSaverDenylisted) {
            // app is denylisted, launch App Data Usage screen
            AppInfoDashboardFragment.startAppInfoFragment(AppDataUsage.class,
                    R.string.data_usage_app_summary_title,
                    null /* arguments */,
                    mParentFragment,
                    mEntry);
        } else {
            // app is not denylisted, let superclass handle toggle switch
            super.onClick();
        }
    }

    @Override
    public void performClick() {
        if (!mHelper.performClick()) {
            super.performClick();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        if (mCacheIcon == null) {
            ThreadUtils.postOnBackgroundThread(() -> {
                final Drawable icon = AppUtils.getIcon(getContext(), mEntry);
                ThreadUtils.postOnMainThread(() -> {
                    setIcon(icon);
                    mCacheIcon = icon;
                });
            });
        }
        final boolean disabledByAdmin = isDisabledByAdmin();
        final View widgetFrame = holder.findViewById(android.R.id.widget_frame);
        if (disabledByAdmin) {
            widgetFrame.setVisibility(View.VISIBLE);
        } else {
            widgetFrame.setVisibility(
                    mDataUsageState != null && mDataUsageState.isDataSaverDenylisted
                            ? View.INVISIBLE : View.VISIBLE);
        }
        super.onBindViewHolder(holder);

        mHelper.onBindViewHolder(holder);
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
    }

    @Override
    public void onAllowlistStatusChanged(int uid, boolean isAllowlisted) {
        if (mDataUsageState != null && mEntry.info.uid == uid) {
            mDataUsageState.isDataSaverAllowlisted = isAllowlisted;
            updateState();
        }
    }

    @Override
    public void onDenylistStatusChanged(int uid, boolean isDenylisted) {
        if (mDataUsageState != null && mEntry.info.uid == uid) {
            mDataUsageState.isDataSaverDenylisted = isDenylisted;
            updateState();
        }
    }

    @Nullable
    public AppStateDataUsageBridge.DataUsageState getDataUsageState() {
        return mDataUsageState;
    }

    public AppEntry getEntry() {
        return mEntry;
    }

    public boolean isDisabledByAdmin() {
        return mHelper.isDisabledByAdmin();
    }

    @VisibleForTesting
    boolean isDisabledByEcm() {
        return mHelper.isDisabledByEcm();
    }

    public void setDisabledByAdmin(EnforcedAdmin admin) {
        mHelper.setDisabledByAdmin(admin);
    }

    /**
     * Checks if the given setting is subject to Enhanced Confirmation Mode restrictions for this
     * package. Marks the preference as disabled if so.
     * @param packageName the package to check the restriction for
     */
    public void checkEcmRestrictionAndSetDisabled(@NonNull String packageName) {
        mHelper.checkEcmRestrictionAndSetDisabled(ECM_SETTING_IDENTIFIER, packageName);
    }

    // Sets UI state based on allowlist/denylist status.
    public void updateState() {
        setTitle(mEntry.label);
        if (mDataUsageState != null) {
            setChecked(mDataUsageState.isDataSaverAllowlisted);
            if (isDisabledByAdmin()) {
                setSummary(com.android.settingslib.widget.restricted.R.string.disabled_by_admin);
            } else if (mDataUsageState.isDataSaverDenylisted) {
                setSummary(R.string.restrict_background_blocklisted);
            // If disabled by ECM, the summary is set directly by the switch.
            } else if (!isDisabledByEcm()) {
                setSummary("");
            }
        }
        notifyChanged();
    }
}
