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

package com.android.settings.fuelgauge;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipDialogFragment;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;

/**
 * Controller to control whether an app can run in the background
 */
public class BackgroundActivityPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "BgActivityPrefContr";
    @VisibleForTesting
    static final String KEY_BACKGROUND_ACTIVITY = "background_activity";

    private final AppOpsManager mAppOpsManager;
    private final UserManager mUserManager;
    private final int mUid;
    @VisibleForTesting
    DevicePolicyManager mDpm;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    private InstrumentedPreferenceFragment mFragment;
    private String mTargetPackage;
    private PowerWhitelistBackend mPowerWhitelistBackend;

    public BackgroundActivityPreferenceController(Context context,
            InstrumentedPreferenceFragment fragment, int uid, String packageName) {
        this(context, fragment, uid, packageName, PowerWhitelistBackend.getInstance(context));
    }

    @VisibleForTesting
    BackgroundActivityPreferenceController(Context context, InstrumentedPreferenceFragment fragment,
            int uid, String packageName, PowerWhitelistBackend backend) {
        super(context);
        mPowerWhitelistBackend = backend;
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mDpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mUid = uid;
        mFragment = fragment;
        mTargetPackage = packageName;
        mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    public void updateState(Preference preference) {
        final RestrictedPreference restrictedPreference = (RestrictedPreference) preference;
        if (restrictedPreference.isDisabledByAdmin()) {
            // If disabled, let RestrictedPreference handle it and do nothing here
            return;
        }
        final int mode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mTargetPackage);
        final boolean whitelisted = mPowerWhitelistBackend.isWhitelisted(mTargetPackage);
        if (whitelisted || mode == AppOpsManager.MODE_ERRORED
                || Utils.isProfileOrDeviceOwner(mUserManager, mDpm, mTargetPackage)) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
        }
        updateSummary(preference);
    }

    @Override
    public boolean isAvailable() {
        return mTargetPackage != null;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BACKGROUND_ACTIVITY;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_BACKGROUND_ACTIVITY.equals(preference.getKey())) {
            final int mode = mAppOpsManager
                    .checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mTargetPackage);
            final boolean restricted = mode == AppOpsManager.MODE_IGNORED;
            showDialog(restricted);
        }

        return false;
    }

    public void updateSummary(Preference preference) {
        if (mPowerWhitelistBackend.isWhitelisted(mTargetPackage)) {
            preference.setSummary(R.string.background_activity_summary_whitelisted);
            return;
        }
        final int mode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mTargetPackage);

        if (mode == AppOpsManager.MODE_ERRORED) {
            preference.setSummary(R.string.background_activity_summary_disabled);
        } else {
            final boolean restricted = mode == AppOpsManager.MODE_IGNORED;
            preference.setSummary(restricted ? R.string.restricted_true_label
                    : R.string.restricted_false_label);
        }
    }

    @VisibleForTesting
    void showDialog(boolean restricted) {
        final AppInfo appInfo = new AppInfo.Builder()
                .setUid(mUid)
                .setPackageName(mTargetPackage)
                .build();
        BatteryTip tip = restricted
                ? new UnrestrictAppTip(BatteryTip.StateType.NEW, appInfo)
                : new RestrictAppTip(BatteryTip.StateType.NEW, appInfo);

        final BatteryTipDialogFragment dialogFragment = BatteryTipDialogFragment.newInstance(tip,
                mFragment.getMetricsCategory());
        dialogFragment.setTargetFragment(mFragment, 0 /* requestCode */);
        dialogFragment.show(mFragment.getFragmentManager(), TAG);
    }
}
