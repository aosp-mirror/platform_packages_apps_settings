/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.connecteddevice;

import android.content.Context;
import android.util.FeatureFlagUtils;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.FeatureFlags;

public class TopLevelConnectedDevicesPreferenceController extends BasePreferenceController {

    public TopLevelConnectedDevicesPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_top_level_connected_devices)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        // Remove homepage summaries for silky home.
        if (FeatureFlagUtils.isEnabled(mContext, FeatureFlags.SILKY_HOME)) {
            return null;
        }

        return mContext.getText(
                AdvancedConnectedDeviceController.getConnectedDevicesSummaryResourceId(mContext));
    }
}
