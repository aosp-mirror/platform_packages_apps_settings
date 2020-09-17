/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.nfc;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
/**
 * Controller that used to show nfc detection point guidance
 */
public class NfcDetectionPointController extends BasePreferenceController {
    private boolean mEnabled;

    public NfcDetectionPointController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mEnabled = mContext.getResources().getBoolean(R.bool.config_nfc_detection_point);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mEnabled) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @VisibleForTesting
    public void setConfig(boolean value) {
        mEnabled = value;
    }
}
