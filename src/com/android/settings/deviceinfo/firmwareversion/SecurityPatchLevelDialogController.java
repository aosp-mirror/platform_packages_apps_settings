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

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.settings.R;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.wrapper.PackageManagerWrapper;

public class SecurityPatchLevelDialogController implements View.OnClickListener {

    private static final String TAG = "SecurityPatchCtrl";
    private static final Uri INTENT_URI_DATA = Uri.parse(
            "https://source.android.com/security/bulletin/");

    @VisibleForTesting
    static final int SECURITY_PATCH_VALUE_ID = R.id.security_patch_level_value;
    @VisibleForTesting
    static final int SECURITY_PATCH_LABEL_ID = R.id.security_patch_level_label;

    private final FirmwareVersionDialogFragment mDialog;
    private final Context mContext;
    private final PackageManagerWrapper mPackageManager;
    private final String mCurrentPatch;

    public SecurityPatchLevelDialogController(FirmwareVersionDialogFragment dialog) {
        mDialog = dialog;
        mContext = dialog.getContext();
        mPackageManager = new PackageManagerWrapper(mContext.getPackageManager());
        mCurrentPatch = DeviceInfoUtils.getSecurityPatch();
    }

    @Override
    public void onClick(View v) {
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(INTENT_URI_DATA);
        if (mPackageManager.queryIntentActivities(intent, 0).isEmpty()) {
            // Don't send out the intent to stop crash
            Log.w(TAG, "Stop click action on " + SECURITY_PATCH_VALUE_ID + ": "
                    + "queryIntentActivities() returns empty");
            return;
        }

        mContext.startActivity(intent);
    }

    /**
     * Populates the security patch level field in the dialog and registers click listeners.
     */
    public void initialize() {
        if (TextUtils.isEmpty(mCurrentPatch)) {
            mDialog.removeSettingFromScreen(SECURITY_PATCH_LABEL_ID);
            mDialog.removeSettingFromScreen(SECURITY_PATCH_VALUE_ID);
            return;
        }
        registerListeners();
        mDialog.setText(SECURITY_PATCH_VALUE_ID, mCurrentPatch);
    }

    private void registerListeners() {
        mDialog.registerClickListener(SECURITY_PATCH_LABEL_ID, this /* listener */);
        mDialog.registerClickListener(SECURITY_PATCH_VALUE_ID, this /* listener */);
    }
}
