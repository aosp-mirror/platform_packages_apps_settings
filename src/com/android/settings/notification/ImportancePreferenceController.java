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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.PreferenceControllerMixin;

public class ImportancePreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_IMPORTANCE = "importance";

    // Ironically doesn't take an importance listener because the importance is not changed
    // by this controller's preference but by the screen it links to.
    public ImportancePreferenceController(Context context) {
        super(context, null);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_IMPORTANCE;
    }
    
    private int getMetricsCategory() {
        return MetricsProto.MetricsEvent.NOTIFICATION_TOPIC_NOTIFICATION;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mChannel == null) {
            return false;
        }
        return !NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId());
    }

    public void updateState(Preference preference) {
        if (mAppRow!= null && mChannel != null) {
            preference.setEnabled(mAdmin == null && isChannelConfigurable());
            Bundle channelArgs = new Bundle();
            channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mAppRow.uid);
            channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mAppRow.pkg);
            channelArgs.putString(Settings.EXTRA_CHANNEL_ID, mChannel.getId());
            if (preference.isEnabled()) {
                Intent channelIntent = Utils.onBuildStartFragmentIntent(mContext,
                        ChannelImportanceSettings.class.getName(),
                        channelArgs, null, R.string.notification_importance_title, null,
                        false, getMetricsCategory());
                preference.setIntent(channelIntent);
                preference.setSummary(getImportanceSummary(mContext, mChannel));
            }
        }
    }

    protected static String getImportanceSummary(Context context, NotificationChannel channel) {
        String title;
        String summary = null;
        int importance = channel.getImportance();
        switch (importance) {
            case IMPORTANCE_UNSPECIFIED:
                title = context.getString(R.string.notification_importance_unspecified);
                break;
            case NotificationManager.IMPORTANCE_MIN:
                title = context.getString(R.string.notification_importance_min_title);
                summary = context.getString(R.string.notification_importance_min);
                break;
            case NotificationManager.IMPORTANCE_LOW:
                title = context.getString(R.string.notification_importance_low_title);
                summary = context.getString(R.string.notification_importance_low);
                break;
            case NotificationManager.IMPORTANCE_DEFAULT:
                title = context.getString(R.string.notification_importance_default_title);
                if (SoundPreferenceController.hasValidSound(channel)) {
                    summary = context.getString(R.string.notification_importance_default);
                } else {
                    summary = context.getString(R.string.notification_importance_low);
                }
                break;
            case NotificationManager.IMPORTANCE_HIGH:
            case NotificationManager.IMPORTANCE_MAX:
                title = context.getString(R.string.notification_importance_high_title);
                if (SoundPreferenceController.hasValidSound(channel)) {
                    summary = context.getString(R.string.notification_importance_high);
                } else {
                    summary = context.getString(R.string.notification_importance_high_silent);
                }
                break;
            default:
                return "";
        }

        if (summary != null) {
            return context.getString(R.string.notification_importance_divider, title, summary);
        } else {
            return title;
        }
    }
}
