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

import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfMeteredDataRestricted;

import android.content.Context;
import android.os.UserHandle;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.AppSwitchPreference;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

public class UnrestrictedDataAccessPreference extends AppSwitchPreference implements
        DataSaverBackend.Listener {

    private final ApplicationsState mApplicationsState;
    private final AppEntry mEntry;
    private final AppStateDataUsageBridge.DataUsageState mDataUsageState;
    private final DataSaverBackend mDataSaverBackend;
    private final DashboardFragment mParentFragment;
    private final RestrictedPreferenceHelper mHelper;

    public UnrestrictedDataAccessPreference(final Context context, AppEntry entry,
            ApplicationsState applicationsState, DataSaverBackend dataSaverBackend,
            DashboardFragment parentFragment) {
        super(context);
        setWidgetLayoutResource(R.layout.restricted_switch_widget);
        mHelper = new RestrictedPreferenceHelper(context, this, null);
        mEntry = entry;
        mDataUsageState = (AppStateDataUsageBridge.DataUsageState) mEntry.extraInfo;
        mEntry.ensureLabel(context);
        mApplicationsState = applicationsState;
        mDataSaverBackend = dataSaverBackend;
        mParentFragment = parentFragment;
        setDisabledByAdmin(checkIfMeteredDataRestricted(context, entry.info.packageName,
                UserHandle.getUserId(entry.info.uid)));
        updateState();
        setKey(generateKey(mEntry));
        if (mEntry.icon != null) {
            setIcon(mEntry.icon);
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
        if (mDataUsageState.isDataSaverBlacklisted) {
            // app is blacklisted, launch App Data Usage screen
            AppInfoDashboardFragment.startAppInfoFragment(AppDataUsage.class,
                    R.string.data_usage_app_summary_title,
                    null /* arguments */,
                    mParentFragment,
                    mEntry);
        } else {
            // app is not blacklisted, let superclass handle toggle switch
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
        if (mEntry.icon == null) {
            holder.itemView.post(new Runnable() {
                @Override
                public void run() {
                    // Ensure we have an icon before binding.
                    mApplicationsState.ensureIcon(mEntry);
                    // This might trigger us to bind again, but it gives an easy way to only
                    // load the icon once its needed, so its probably worth it.
                    setIcon(mEntry.icon);
                }
            });
        }
        final boolean disabledByAdmin = isDisabledByAdmin();
        final View widgetFrame = holder.findViewById(android.R.id.widget_frame);
        if (disabledByAdmin) {
            widgetFrame.setVisibility(View.VISIBLE);
        } else {
            widgetFrame.setVisibility(
                    mDataUsageState != null && mDataUsageState.isDataSaverBlacklisted
                            ? View.INVISIBLE : View.VISIBLE);
        }
        super.onBindViewHolder(holder);

        mHelper.onBindViewHolder(holder);
        holder.findViewById(R.id.restricted_icon).setVisibility(
                disabledByAdmin ? View.VISIBLE : View.GONE);
        holder.findViewById(android.R.id.switch_widget).setVisibility(
                disabledByAdmin ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
        if (mDataUsageState != null && mEntry.info.uid == uid) {
            mDataUsageState.isDataSaverWhitelisted = isWhitelisted;
            updateState();
        }
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
        if (mDataUsageState != null && mEntry.info.uid == uid) {
            mDataUsageState.isDataSaverBlacklisted = isBlacklisted;
            updateState();
        }
    }

    public AppStateDataUsageBridge.DataUsageState getDataUsageState() {
        return mDataUsageState;
    }

    public AppEntry getEntry() {
        return mEntry;
    }

    public boolean isDisabledByAdmin() {
        return mHelper.isDisabledByAdmin();
    }

    public void setDisabledByAdmin(EnforcedAdmin admin) {
        mHelper.setDisabledByAdmin(admin);
    }

    // Sets UI state based on whitelist/blacklist status.
    public void updateState() {
        setTitle(mEntry.label);
        if (mDataUsageState != null) {
            setChecked(mDataUsageState.isDataSaverWhitelisted);
            if (isDisabledByAdmin()) {
                setSummary(R.string.disabled_by_admin);
            } else if (mDataUsageState.isDataSaverBlacklisted) {
                setSummary(R.string.restrict_background_blacklisted);
            } else {
                setSummary("");
            }
        }
        notifyChanged();
    }
}
