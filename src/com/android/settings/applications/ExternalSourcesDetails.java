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

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.applications.AppStateInstallAppsBridge.InstallAppsState;

public class ExternalSourcesDetails extends AppInfoWithHeader
        implements OnPreferenceChangeListener {

    private static final String KEY_EXTERNAL_SOURCES_SETTINGS_SWITCH =
            "external_sources_settings_switch";
    private static final String KEY_EXTERNAL_SOURCES_SETTINGS_DESC =
            "external_sources_settings_description";

    private AppStateInstallAppsBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private SwitchPreference mSwitchPref;
    private Preference mExternalSourcesSettingsDesc;
    private InstallAppsState mInstallAppsState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getActivity();
        mAppBridge = new AppStateInstallAppsBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        addPreferencesFromResource(R.xml.external_sources_details);
        mSwitchPref = (SwitchPreference) findPreference(KEY_EXTERNAL_SOURCES_SETTINGS_SWITCH);
        mExternalSourcesSettingsDesc = findPreference(KEY_EXTERNAL_SOURCES_SETTINGS_DESC);

        getPreferenceScreen().setTitle(R.string.install_other_apps);
        mSwitchPref.setTitle(R.string.external_source_switch_title);
        mExternalSourcesSettingsDesc.setSummary(R.string.install_all_warning);

        mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean checked = (Boolean) newValue;
        if (preference == mSwitchPref) {
            if (mInstallAppsState != null && checked != mInstallAppsState.canInstallApps()) {
                setCanInstallApps(checked);
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setCanInstallApps(boolean newState) {
        mAppOpsManager.setMode(AppOpsManager.OP_REQUEST_INSTALL_PACKAGES,
                mPackageInfo.applicationInfo.uid, mPackageName,
                newState ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
    }

    @Override
    protected boolean refreshUi() {
        mInstallAppsState = mAppBridge.createInstallAppsStateFor(mPackageName,
                mPackageInfo.applicationInfo.uid);

        final boolean canWrite = mInstallAppsState.canInstallApps();
        mSwitchPref.setChecked(canWrite);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.MANAGE_EXTERNAL_SOURCES;
    }
}
