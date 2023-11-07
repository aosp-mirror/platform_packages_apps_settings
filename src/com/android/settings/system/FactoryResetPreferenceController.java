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
 * limitations under the License
 */
package com.android.settings.system;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import com.android.settings.Settings;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.factory_reset.Flags;

public class FactoryResetPreferenceController extends BasePreferenceController {

    private static final String TAG = "FactoryResetPreference";

    private static final String ACTION_PREPARE_FACTORY_RESET =
            "com.android.settings.ACTION_PREPARE_FACTORY_RESET";

    private final UserManager mUm;
    private ActivityResultLauncher<Intent> mFactoryResetPreparationLauncher;

    public FactoryResetPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    /** Hide "Factory reset" settings for secondary users. */
    @Override
    public int getAvailabilityStatus() {
        return mUm.isAdminUser() ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (mPreferenceKey.equals(preference.getKey())) {
            if (Flags.enableFactoryResetWizard()) {
                startFactoryResetPreparationActivity();
            } else {
                startFactoryResetActivity();
            }
            return true;
        }
        return false;
    }

    private void startFactoryResetPreparationActivity() {
        Intent prepareFactoryResetIntent = getPrepareFactoryResetIntent();
        if (prepareFactoryResetIntent != null && mFactoryResetPreparationLauncher != null) {
            mFactoryResetPreparationLauncher.launch(prepareFactoryResetIntent);
        } else {
            startFactoryResetActivity();
        }
    }

    // We check that the activity that can handle the factory reset preparation action is indeed
    // a system app with proper permissions.
    private Intent getPrepareFactoryResetIntent() {
        final Intent prepareFactoryResetWizardRequest = new Intent(ACTION_PREPARE_FACTORY_RESET);
        final PackageManager pm = mContext.getPackageManager();
        final ResolveInfo resolution = pm.resolveActivity(prepareFactoryResetWizardRequest, 0);
        if (resolution != null
                && resolution.activityInfo != null) {
            String packageName = resolution.activityInfo.packageName;
            PackageInfo factoryResetWizardPackageInfo;
            try {
                factoryResetWizardPackageInfo = pm.getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to resolve a Factory Reset Handler Application");
                return null;
            }
            if (factoryResetWizardPackageInfo.requestedPermissions == null
                    || factoryResetWizardPackageInfo.requestedPermissions.length == 0) {
                Log.e(TAG, "Factory Reset Handler has no permissions requested.");
                return null;
            }
            for (int i = 0; i < factoryResetWizardPackageInfo.requestedPermissions.length; i++) {
                String permission = factoryResetWizardPackageInfo.requestedPermissions[i];
                boolean isGranted =
                        (factoryResetWizardPackageInfo.requestedPermissionsFlags[i]
                                & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                if (permission.equals(Manifest.permission.PREPARE_FACTORY_RESET) && isGranted) {
                    return prepareFactoryResetWizardRequest;
                }
            }
            return prepareFactoryResetWizardRequest;
        }
        Log.i(TAG, "Unable to resolve a Factory Reset Handler Activity");
        return null;
    }

    void setFragment(ResetDashboardFragment fragment) {
        if (Flags.enableFactoryResetWizard()) {
            mFactoryResetPreparationLauncher = fragment.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        startFactoryResetActivity();
                    });
        }
    }

    private void startFactoryResetActivity() {
        final Intent intent = new Intent(mContext, Settings.FactoryResetActivity.class);
        mContext.startActivity(intent);
    }
}
