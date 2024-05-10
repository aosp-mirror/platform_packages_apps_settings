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

import static com.android.settings.development.DevelopmentOptionsActivityRequestCodes.REQUEST_CODE_DEBUG_APP;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class SelectDebugAppPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin, OnActivityResultListener {

    private static final String DEBUG_APP_KEY = "debug_app";

    private final DevelopmentSettingsDashboardFragment mFragment;
    private final PackageManager mPackageManager;

    public SelectDebugAppPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public String getPreferenceKey() {
        return DEBUG_APP_KEY;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        if (Flags.deprecateListActivity()) {
            final Bundle args = new Bundle();
            args.putBoolean(DevelopmentAppPicker.EXTRA_DEBUGGABLE, true /* value */);
            final String debugApp = Settings.Global.getString(
                    mContext.getContentResolver(), Settings.Global.DEBUG_APP);
            args.putString(DevelopmentAppPicker.EXTRA_SELECTING_APP, debugApp);
            new SubSettingLauncher(mContext)
                    .setDestination(DevelopmentAppPicker.class.getName())
                    .setSourceMetricsCategory(SettingsEnums.DEVELOPMENT)
                    .setArguments(args)
                    .setTitleRes(com.android.settingslib.R.string.select_application)
                    .setResultListener(mFragment, REQUEST_CODE_DEBUG_APP)
                    .launch();
        } else {
            final Intent intent = getActivityStartIntent();
            intent.putExtra(AppPicker.EXTRA_DEBUGGABLE, true /* value */);
            mFragment.startActivityForResult(intent, REQUEST_CODE_DEBUG_APP);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updatePreferenceSummary();
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_DEBUG_APP || resultCode != Activity.RESULT_OK) {
            return false;
        }
        Settings.Global.putString(mContext.getContentResolver(), Settings.Global.DEBUG_APP,
                data.getAction());
        updatePreferenceSummary();
        return true;
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        mPreference.setSummary(mContext.getResources()
                .getString(com.android.settingslib.R.string.debug_app_not_set));
    }

    @VisibleForTesting
    Intent getActivityStartIntent() {
        return new Intent(mContext, AppPicker.class);
    }

    private void updatePreferenceSummary() {
        final String debugApp = Settings.Global.getString(
                mContext.getContentResolver(), Settings.Global.DEBUG_APP);
        if (debugApp != null && debugApp.length() > 0) {
            mPreference.setSummary(mContext.getResources()
                    .getString(com.android.settingslib.R.string.debug_app_set,
                            getAppLabel(debugApp)));
        } else {
            mPreference.setSummary(mContext.getResources()
                    .getString(com.android.settingslib.R.string.debug_app_not_set));
        }
    }

    private String getAppLabel(String debugApp) {
        try {
            final ApplicationInfo ai = mPackageManager.getApplicationInfo(debugApp,
                    PackageManager.GET_DISABLED_COMPONENTS);
            final CharSequence lab = mPackageManager.getApplicationLabel(ai);
            return lab != null ? lab.toString() : debugApp;
        } catch (PackageManager.NameNotFoundException e) {
            return debugApp;
        }
    }
}
