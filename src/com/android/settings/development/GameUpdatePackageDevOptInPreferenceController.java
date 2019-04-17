/*
 * Copyright 2018 The Android Open Source Project
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

import static com.android.settings.development.DevelopmentOptionsActivityRequestCodes
        .REQUEST_CODE_GUP_DEV_OPT_IN_APPS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

// TODO(b/119221883): Need to override isAvailable() to return false when updatable graphics driver is not supported.
public class GameUpdatePackageDevOptInPreferenceController
        extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin, OnActivityResultListener {

    private static final String GUP_DEV_OPT_IN_APP_KEY = "gup_dev_opt_in_app";

    private final DevelopmentSettingsDashboardFragment mFragment;
    private final PackageManager mPackageManager;

    public GameUpdatePackageDevOptInPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public String getPreferenceKey() {
        return GUP_DEV_OPT_IN_APP_KEY;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (GUP_DEV_OPT_IN_APP_KEY.equals(preference.getKey())) {
            // pass it on to settings
            final Intent intent = getActivityStartIntent();
            mFragment.startActivityForResult(intent, REQUEST_CODE_GUP_DEV_OPT_IN_APPS);
            return true;
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        updatePreferenceSummary();
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_GUP_DEV_OPT_IN_APPS
                || resultCode != Activity.RESULT_OK) {
            return false;
        }
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.GUP_DEV_OPT_IN_APPS, data.getAction());
        updatePreferenceSummary();
        return true;
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        mPreference.setSummary(mContext.getResources().getString(
                R.string.gup_dev_opt_in_app_not_set));
    }

    @VisibleForTesting
    Intent getActivityStartIntent() {
        Intent intent = new Intent(mContext, AppPicker.class);
        intent.putExtra(AppPicker.EXTRA_DEBUGGABLE, true /* value */);
        return intent;
    }

    private void updatePreferenceSummary() {
        final String optInApp = Settings.Global.getString(
                mContext.getContentResolver(), Settings.Global.GUP_DEV_OPT_IN_APPS);
        if (optInApp != null && !optInApp.isEmpty()) {
            mPreference.setSummary(mContext.getResources().getString(
                    R.string.gup_dev_opt_in_app_set, getAppLabel(optInApp)));
        } else {
            mPreference.setSummary(mContext.getResources().getString(
                    R.string.gup_dev_opt_in_app_not_set));
        }
    }

    private String getAppLabel(String applicationPackageName) {
        try {
            final ApplicationInfo ai = mPackageManager.getApplicationInfo(applicationPackageName,
                    PackageManager.GET_DISABLED_COMPONENTS);
            final CharSequence lab = mPackageManager.getApplicationLabel(ai);
            return lab != null ? lab.toString() : applicationPackageName;
        } catch (PackageManager.NameNotFoundException e) {
            return applicationPackageName;
        }
    }
}
