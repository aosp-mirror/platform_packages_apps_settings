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
package com.android.settings.applications;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_ERRORED;
import static android.app.AppOpsManager.OP_PICTURE_IN_PICTURE;

public class PictureInPictureDetails extends AppInfoWithHeader
        implements OnPreferenceChangeListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String KEY_APP_OPS_SETTINGS_PREFS = "app_ops_settings_preference";
    private static final String KEY_APP_OPS_SETTINGS_DESC = "app_ops_settings_description";
    private static final String LOG_TAG = "PictureInPictureDetails";

    private SwitchPreference mSwitchPref;
    private Preference mOverlayDesc;
    private Intent mSettingsIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // find preferences
        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        mSwitchPref = (SwitchPreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);
        mOverlayDesc = findPreference(KEY_APP_OPS_SETTINGS_DESC);
        getPreferenceScreen().removePreference(findPreference(KEY_APP_OPS_SETTINGS_PREFS));

        // set title/summary for all of them
        getPreferenceScreen().setTitle(R.string.picture_in_picture_app_detail_title);
        mSwitchPref.setTitle(R.string.picture_in_picture_app_detail_switch);
        mOverlayDesc.setSummary(R.string.picture_in_picture_app_detail_summary);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);

        mSettingsIntent = new Intent(Intent.ACTION_MAIN)
                .setAction(Settings.ACTION_PICTURE_IN_PICTURE_SETTINGS);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchPref) {
            logSpecialPermissionChange((Boolean) newValue, mPackageName);
            setEnterPipStateForPackage(getActivity(), mPackageInfo.applicationInfo.uid, mPackageName,
                    (Boolean) newValue);
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
        return MetricsEvent.SETTINGS_MANAGE_PICTURE_IN_PICTURE;
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
     *         picture-in-picture.
     */
    static boolean getEnterPipStateForPackage(Context context, int uid, String packageName) {
        final AppOpsManager appOps = context.getSystemService(AppOpsManager.class);
        return appOps.checkOpNoThrow(OP_PICTURE_IN_PICTURE, uid, packageName) == MODE_ALLOWED;
    }

    /**
     * @return the summary for the current state of whether the app associated with the given
     *         {@param packageName} is allowed to enter picture-in-picture.
     */
    static int getPreferenceSummary(Context context, int uid, String packageName) {
        final boolean enabled = PictureInPictureDetails.getEnterPipStateForPackage(context, uid,
                packageName);
        return enabled ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed;
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean newState, String packageName) {
        int logCategory = newState
                ? MetricsEvent.APP_PICTURE_IN_PICTURE_ALLOW
                : MetricsEvent.APP_PICTURE_IN_PICTURE_DENY;
        FeatureFactory.getFactory(getContext())
                .getMetricsFeatureProvider().action(getContext(), logCategory, packageName);
    }
}
