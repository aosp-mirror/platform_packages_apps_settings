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
 * limitations under the License
 */

package com.android.settings.display;

import static com.android.settings.display.AdaptiveSleepPreferenceController.hasSufficientPermission;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class AdaptiveSleepPermissionPreferenceController extends BasePreferenceController {
    final static String PREF_NAME = "adaptive_sleep_permission";
    private final Intent mIntent;

    public AdaptiveSleepPermissionPreferenceController(Context context, String key) {
        super(context, key);
        final String packageName = context.getPackageManager().getAttentionServicePackageName();
        mIntent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        mIntent.setData(Uri.parse("package:" + packageName));
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            mContext.startActivity(mIntent);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            preference.setVisible(!hasSufficientPermission(mContext.getPackageManager()));
        }
    }
}
