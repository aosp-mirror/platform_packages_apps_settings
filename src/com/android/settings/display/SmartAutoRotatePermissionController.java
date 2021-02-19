/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.display.SmartAutoRotateController.hasSufficientPermission;
import static com.android.settings.display.SmartAutoRotateController.isRotationResolverServiceAvailable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

/**
 * The controller of camera based rotate permission warning preference. The preference appears when
 * the camera permission is missing for the camera based rotation feature.
 */
public class SmartAutoRotatePermissionController extends BasePreferenceController {

    private final Intent mIntent;

    public SmartAutoRotatePermissionController(Context context, String key) {
        super(context, key);
        final String packageName = context.getPackageManager().getRotationResolverPackageName();
        mIntent = new Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        mIntent.setData(Uri.parse("package:" + packageName));
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return isRotationResolverServiceAvailable(mContext) && !hasSufficientPermission(mContext)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            mContext.startActivity(mIntent);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }
}
