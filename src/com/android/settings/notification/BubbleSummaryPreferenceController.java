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

package com.android.settings.notification;

import static android.provider.Settings.Global.NOTIFICATION_BUBBLES;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.SubSettingLauncher;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

public class BubbleSummaryPreferenceController extends NotificationPreferenceController {

    private static final String KEY = "bubble_link_pref";
    @VisibleForTesting
    static final int SYSTEM_WIDE_ON = 1;
    @VisibleForTesting
    static final int SYSTEM_WIDE_OFF = 0;

    public BubbleSummaryPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mAppRow == null && mChannel == null) {
            return false;
        }
        if (mChannel != null) {
            if (!isGloballyEnabled()) {
                return false;
            }
            if (isDefaultChannel()) {
                return true;
            } else {
                return mAppRow != null && mAppRow.allowBubbles;
            }
        }
        return isGloballyEnabled();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (mAppRow != null) {
            Bundle args = new Bundle();
            args.putString(AppInfoBase.ARG_PACKAGE_NAME, mAppRow.pkg);
            args.putInt(AppInfoBase.ARG_PACKAGE_UID, mAppRow.uid);

            preference.setIntent(new SubSettingLauncher(mContext)
                    .setDestination(AppBubbleNotificationSettings.class.getName())
                    .setArguments(args)
                    .setSourceMetricsCategory(
                            SettingsEnums.NOTIFICATION_APP_NOTIFICATION)
                    .toIntent());
        }
    }

    @Override
    public CharSequence getSummary() {
        boolean canBubble = false;
        if (mAppRow != null) {
            if (mChannel != null) {
               canBubble |= mChannel.canBubble() && isGloballyEnabled();
            } else {
               canBubble |= mAppRow.allowBubbles && isGloballyEnabled();
            }
        }
        return mContext.getString(canBubble ? R.string.switch_on_text : R.string.switch_off_text);
    }

    private boolean isGloballyEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF) == SYSTEM_WIDE_ON;
    }
}
