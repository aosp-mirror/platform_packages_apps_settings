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
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;

public class BasebandVersionDialogController {

    @VisibleForTesting
    static final int BASEBAND_VERSION_LABEL_ID = R.id.baseband_version_label;
    @VisibleForTesting
    static final int BASEBAND_VERSION_VALUE_ID = R.id.baseband_version_value;
    @VisibleForTesting
    static final String BASEBAND_PROPERTY = "gsm.version.baseband";

    private final FirmwareVersionDialogFragment mDialog;

    public BasebandVersionDialogController(FirmwareVersionDialogFragment dialog) {
        mDialog = dialog;
    }

    /**
     * Updates the baseband version field of the dialog.
     */
    public void initialize() {
        final Context context = mDialog.getContext();
        if (Utils.isWifiOnly(context)) {
            mDialog.removeSettingFromScreen(BASEBAND_VERSION_LABEL_ID);
            mDialog.removeSettingFromScreen(BASEBAND_VERSION_VALUE_ID);
            return;
        }

        mDialog.setText(BASEBAND_VERSION_VALUE_ID, SystemProperties.get(BASEBAND_PROPERTY,
                context.getString(R.string.device_info_default)));
    }
}
