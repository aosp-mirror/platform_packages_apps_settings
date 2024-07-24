/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.settings.applications.specialaccess;

import android.Manifest;
import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.media.flags.Flags;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.spa.SpaActivity;
import com.android.settings.spa.app.specialaccess.MediaRoutingControlAppListProvider;

/**
 * This controller manages features availability for special app access for
 * {@link Manifest.permission#MEDIA_ROUTING_CONTROL} permission.
 */
public class MediaRoutingControlPreferenceController extends BasePreferenceController {
    public MediaRoutingControlPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enablePrivilegedRoutingForMediaRoutingControl()
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), mPreferenceKey)) {
            SpaActivity.startSpaActivity(
                    mContext, MediaRoutingControlAppListProvider.INSTANCE.getAppListRoute());
            return true;
        }
        return false;
    }
}
