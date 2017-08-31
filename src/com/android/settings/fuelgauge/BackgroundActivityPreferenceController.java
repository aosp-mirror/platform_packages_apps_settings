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

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.wrapper.DevicePolicyManagerWrapper;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Controller to control whether an app can run in the background
 */
public class BackgroundActivityPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "BgActivityPrefContr";
    private static final String KEY_BACKGROUND_ACTIVITY = "background_activity";

    private final PackageManager mPackageManager;
    private final AppOpsManager mAppOpsManager;
    private final UserManager mUserManager;
    private final int mUid;
    @VisibleForTesting
    DevicePolicyManagerWrapper mDpm;
    private Fragment mFragment;
    private String mTargetPackage;
    private boolean mIsPreOApp;
    private PowerWhitelistBackend mPowerWhitelistBackend;

    public BackgroundActivityPreferenceController(Context context, Fragment fragment,
            int uid, String packageName) {
        this(context, fragment, uid, packageName, PowerWhitelistBackend.getInstance());
    }

    @VisibleForTesting
    BackgroundActivityPreferenceController(Context context, Fragment fragment,
            int uid, String packageName, PowerWhitelistBackend backend) {
        super(context);
        mPowerWhitelistBackend = backend;
        mPackageManager = context.getPackageManager();
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mDpm = new DevicePolicyManagerWrapper(
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mUid = uid;
        mFragment = fragment;
        mTargetPackage = packageName;
        mIsPreOApp = isLegacyApp(packageName);
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mTargetPackage);
        final boolean whitelisted = mPowerWhitelistBackend.isWhitelisted(mTargetPackage);
        // Set checked or not before we may set it disabled
        if (mode != AppOpsManager.MODE_ERRORED) {
            final boolean checked = whitelisted || mode != AppOpsManager.MODE_IGNORED;
            ((SwitchPreference) preference).setChecked(checked);
        }
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

    /**
     * Called from the warning dialog, if the user decides to go ahead and disable background
     * activity for this package
     */
    public void setUnchecked(Preference preference) {
        if (mIsPreOApp) {
            mAppOpsManager.setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, mUid, mTargetPackage,
                    AppOpsManager.MODE_IGNORED);
        }
        mAppOpsManager.setMode(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mTargetPackage,
                AppOpsManager.MODE_IGNORED);
        ((SwitchPreference) preference).setChecked(false);
        updateSummary(preference);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BACKGROUND_ACTIVITY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean switchOn = (Boolean) newValue;
        if (!switchOn) {
            final WarningDialogFragment dialogFragment = new WarningDialogFragment();
            dialogFragment.setTargetFragment(mFragment, 0);
            dialogFragment.show(mFragment.getFragmentManager(), TAG);
            return false;
        }
        if (mIsPreOApp) {
            mAppOpsManager.setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, mUid, mTargetPackage,
                    AppOpsManager.MODE_ALLOWED);
        }
        mAppOpsManager.setMode(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mTargetPackage,
                AppOpsManager.MODE_ALLOWED);
        updateSummary(preference);
        return true;
    }

    @VisibleForTesting
    boolean isLegacyApp(final String packageName) {
        try {
            ApplicationInfo info = mPackageManager.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA);

            return info.targetSdkVersion < Build.VERSION_CODES.O;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find package: " + packageName, e);
        }

        return false;
    }

    @VisibleForTesting
    void updateSummary(Preference preference) {
        if (mPowerWhitelistBackend.isWhitelisted(mTargetPackage)) {
            preference.setSummary(R.string.background_activity_summary_whitelisted);
            return;
        }
        final int mode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mTargetPackage);

        if (mode == AppOpsManager.MODE_ERRORED) {
            preference.setSummary(R.string.background_activity_summary_disabled);
        } else {
            final boolean checked = mode != AppOpsManager.MODE_IGNORED;
            preference.setSummary(checked ? R.string.background_activity_summary_on
                    : R.string.background_activity_summary_off);
        }
    }

    interface WarningConfirmationListener {
        void onLimitBackgroundActivity();
    }

    /**
     * Warning dialog to show to the user as turning off background activity can lead to
     * apps misbehaving as their background task scheduling guarantees will no longer be honored.
     */
    public static class WarningDialogFragment extends InstrumentedDialogFragment {
        @Override
        public int getMetricsCategory() {
            // TODO (b/65494831): add metric id
            return 0;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final WarningConfirmationListener listener =
                    (WarningConfirmationListener) getTargetFragment();
            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.background_activity_warning_dialog_title)
                    .setMessage(R.string.background_activity_warning_dialog_text)
                    .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onLimitBackgroundActivity();
                        }
                    })
                    .setNegativeButton(R.string.dlg_cancel, null)
                    .create();
        }
    }
}
