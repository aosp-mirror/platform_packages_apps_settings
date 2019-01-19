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
package com.android.settings.applications.specialaccess.pictureinpicture;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_ERRORED;
import static android.app.AppOpsManager.OP_PICTURE_IN_PICTURE;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class PictureInPictureDetails extends AppInfoWithHeader
        implements OnPreferenceChangeListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String LOG_TAG = "PictureInPictureDetails";

    private SwitchPreference mSwitchPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // find preferences
        addPreferencesFromResource(R.xml.picture_in_picture_permissions_details);
        mSwitchPref = (SwitchPreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);

        // set title/summary for all of them
        mSwitchPref.setTitle(R.string.picture_in_picture_app_detail_switch);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchPref) {
            logSpecialPermissionChange((Boolean) newValue, mPackageName);
            setEnterPipStateForPackage(getActivity(), mPackageInfo.applicationInfo.uid,
                    mPackageName, (Boolean) newValue);
            return true;
        }
        return false;
    }

    @Override
    protected boolean refreshUi() {
        boolean isAllowed = getEnterPipStateForPackage(getActivity(),
                mPackageInfo.applicationInfo.uid, mPackageName);
        mSwitchPref.setChecked(isAllowed);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_MANAGE_PICTURE_IN_PICTURE;
    }

    /**
     * Sets whether the app associated with the given {@param packageName} is allowed to enter
     * picture-in-picture.
     */
    static void setEnterPipStateForPackage(Context context, int uid, String packageName,
            boolean value) {
        final AppOpsManager appOps = context.getSystemService(AppOpsManager.class);
        final int newMode = value ? MODE_ALLOWED : MODE_ERRORED;
        appOps.setMode(OP_PICTURE_IN_PICTURE, uid, packageName, newMode);
    }

    /**
     * @return whether the app associated with the given {@param packageName} is allowed to enter
     * picture-in-picture.
     */
    static boolean getEnterPipStateForPackage(Context context, int uid, String packageName) {
        final AppOpsManager appOps = context.getSystemService(AppOpsManager.class);
        return appOps.checkOpNoThrow(OP_PICTURE_IN_PICTURE, uid, packageName) == MODE_ALLOWED;
    }

    /**
     * @return the summary for the current state of whether the app associated with the given
     * {@param packageName} is allowed to enter picture-in-picture.
     */
    public static int getPreferenceSummary(Context context, int uid, String packageName) {
        final boolean enabled = PictureInPictureDetails.getEnterPipStateForPackage(context, uid,
                packageName);
        return enabled ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed;
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean newState, String packageName) {
        int logCategory = newState
                ? SettingsEnums.APP_PICTURE_IN_PICTURE_ALLOW
                : SettingsEnums.APP_PICTURE_IN_PICTURE_DENY;
        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider();
        metricsFeatureProvider.action(
                metricsFeatureProvider.getAttribution(getActivity()),
                logCategory,
                getMetricsCategory(),
                packageName,
                0);
    }
}
