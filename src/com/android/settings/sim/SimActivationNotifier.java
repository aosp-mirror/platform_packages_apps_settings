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

package com.android.settings.sim;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.IntDef;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.TaskStackBuilder;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.network.SubscriptionUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class manages the notification of SIM activation notification including creating and
 * canceling the notifications.
 */
public class SimActivationNotifier {

    private static final String TAG = "SimActivationNotifier";
    private static final String SIM_SETUP_CHANNEL_ID = "sim_setup";
    private static final String SIM_PREFS = "sim_prefs";
    private static final String KEY_SHOW_SIM_SETTINGS_NOTIFICATION =
            "show_sim_settings_notification";

    public static final int SIM_ACTIVATION_NOTIFICATION_ID = 1;

    /** Notification types */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                NotificationType.NETWORK_CONFIG,
            })
    public @interface NotificationType {
        // The notification to remind users to config network Settings.
        int NETWORK_CONFIG = 1;
    }

    private final Context mContext;
    private final NotificationManager mNotificationManager;

    public SimActivationNotifier(Context context) {
        mContext = context;
        mNotificationManager = context.getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(
                new NotificationChannel(
                        SIM_SETUP_CHANNEL_ID,
                        mContext.getString(R.string.sim_setup_channel_id),
                        NotificationManager.IMPORTANCE_HIGH));
    }

    /**
     * Sets whether Settings should send a push notification for the SIM activation.
     *
     * @param context
     * @param showNotification whether Settings should send a push notification for the SIM
     *     activation.
     */
    public static void setShowSimSettingsNotification(Context context, boolean showNotification) {
        final SharedPreferences prefs = context.getSharedPreferences(SIM_PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SHOW_SIM_SETTINGS_NOTIFICATION, showNotification).apply();
    }

    /**
     * Gets whether Settings should send a push notification for the SIM activation.
     *
     * @param context
     * @return true if Settings should send a push notification for SIM activation. Otherwise,
     *     return false.
     */
    public static boolean getShowSimSettingsNotification(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(SIM_PREFS, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOW_SIM_SETTINGS_NOTIFICATION, false);
    }

    /** Sends a push notification for the SIM activation. It should be called after DSDS reboot. */
    public void sendNetworkConfigNotification() {
        SubscriptionManager subscriptionManager =
                mContext.getSystemService(SubscriptionManager.class);
        SubscriptionInfo activeRemovableSub =
                SubscriptionUtil.getActiveSubscriptions(subscriptionManager).stream()
                        .filter(sub -> !sub.isEmbedded())
                        .findFirst()
                        .orElse(null);

        if (activeRemovableSub == null) {
            Log.e(TAG, "No removable subscriptions found. Do not show notification.");
            return;
        }

        String carrierName =
                TextUtils.isEmpty(activeRemovableSub.getDisplayName())
                        ? mContext.getString(R.string.sim_card_label)
                        : activeRemovableSub.getDisplayName().toString();
        String title =
                mContext.getString(
                        R.string.post_dsds_reboot_notification_title_with_carrier, carrierName);
        String text = mContext.getString(R.string.post_dsds_reboot_notification_text);
        Intent clickIntent = new Intent(mContext, Settings.MobileNetworkListActivity.class);
        TaskStackBuilder stackBuilder =
                TaskStackBuilder.create(mContext).addNextIntent(clickIntent);
        PendingIntent contentIntent =
                stackBuilder.getPendingIntent(
                        0 /* requestCode */, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder =
                new Notification.Builder(mContext, SIM_SETUP_CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setContentIntent(contentIntent)
                        .setSmallIcon(R.drawable.ic_sim_alert)
                        .setAutoCancel(true);
        mNotificationManager.notify(SIM_ACTIVATION_NOTIFICATION_ID, builder.build());
    }
}
