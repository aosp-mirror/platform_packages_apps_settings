/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.Flags;
import android.content.Context;
import android.service.notification.Adjustment;

import androidx.annotation.NonNull;

import com.android.settings.widget.SettingsMainSwitchPreferenceController;

public class BundleTypePreferenceController extends
        SettingsMainSwitchPreferenceController {

    static final String PROMO_KEY = "promotions";
    static final String NEWS_KEY = "news";
    static final String SOCIAL_KEY = "social";
    static final String RECS_KEY = "recs";

    NotificationBackend mBackend;
    int mType;

    public BundleTypePreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mBackend = new NotificationBackend();
        mType = getBundleTypeForKey();
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.notificationClassificationUi() && mBackend.isNotificationBundlingSupported()
                && mBackend.isNotificationBundlingEnabled(mContext)) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return mBackend.isBundleTypeApproved(mType);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mBackend.setBundleTypeState(mType, isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }

    private @Adjustment.Types int getBundleTypeForKey() {
        if (PROMO_KEY.equals(mPreferenceKey)) {
            return Adjustment.TYPE_PROMOTION;
        } else if (NEWS_KEY.equals(mPreferenceKey)) {
            return Adjustment.TYPE_NEWS;
        } else if (SOCIAL_KEY.equals(mPreferenceKey)) {
            return Adjustment.TYPE_SOCIAL_MEDIA;
        } else if (RECS_KEY.equals(mPreferenceKey)) {
            return Adjustment.TYPE_CONTENT_RECOMMENDATION;
        }
        return Adjustment.TYPE_OTHER;
    }
}
