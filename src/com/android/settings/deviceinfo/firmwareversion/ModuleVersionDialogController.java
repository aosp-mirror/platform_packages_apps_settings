/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;

import androidx.annotation.VisibleForTesting;

public class ModuleVersionDialogController {

    private static final String TAG = "MainlineModuleControl";

    @VisibleForTesting
    static final int MODULE_VERSION_LABEL_ID = R.id.module_version_label;
    @VisibleForTesting
    static final int MODULE_VERSION_VALUE_ID = R.id.module_version_value;

    private final FirmwareVersionDialogFragment mDialog;
    private final Context mContext;
    private final PackageManager mPackageManager;

    public ModuleVersionDialogController(FirmwareVersionDialogFragment dialog) {
        mDialog = dialog;
        mContext = mDialog.getContext();
        mPackageManager = mContext.getPackageManager();
    }

    /**
     * Updates the mainline module version field of the dialog.
     */
    public void initialize() {
        if (!FeatureFlagUtils.isEnabled(mContext, FeatureFlags.MAINLINE_MODULE)) {
            mDialog.removeSettingFromScreen(MODULE_VERSION_LABEL_ID);
            mDialog.removeSettingFromScreen(MODULE_VERSION_VALUE_ID);
            return;
        }
        final String moduleProvider = mContext.getString(
            com.android.internal.R.string.config_defaultModuleMetadataProvider);
        if (!TextUtils.isEmpty(moduleProvider)) {
            try {
                mDialog.setText(MODULE_VERSION_VALUE_ID,
                    mPackageManager.getPackageInfo(moduleProvider, 0 /* flags */).versionName);
                return;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Failed to get mainline version.", e);
            }
        }
        mDialog.removeSettingFromScreen(MODULE_VERSION_LABEL_ID);
        mDialog.removeSettingFromScreen(MODULE_VERSION_VALUE_ID);
    }
}
