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

package com.android.settings.accessibility;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import java.util.List;

public class LiveCaptionPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    static final Intent LIVE_CAPTION_INTENT = new Intent(
            "com.android.settings.action.live_caption");

    private final PackageManager mPackageManager;

    public LiveCaptionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        final List<ResolveInfo> resolved =
                mPackageManager.queryIntentActivities(LIVE_CAPTION_INTENT, 0 /* flags */);
        return resolved != null && !resolved.isEmpty()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setIntent(LIVE_CAPTION_INTENT);
    }
}