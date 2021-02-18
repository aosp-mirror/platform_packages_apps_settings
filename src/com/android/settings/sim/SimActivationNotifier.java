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
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.TaskStackBuilder;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.network.SubscriptionUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nullable;

/**
 * This class manages the notification of SIM activation notification including creating and
 * canceling the notifications.
 */
public class SimActivationNotifier {

    private static final String TAG = "SimActivationNotifier";
    private static final String SIM_SETUP_CHANNEL_ID = "sim_setup";
    private static final String SWITCH_SLOT_CHANNEL_ID = "carrier_switching";
    private static final String SIM_PREFS = "sim_prefs";
    private static final String KEY_SHOW_SIM_SETTINGS_NOTIFICATION =
            "show_sim_settings_notification";

    public static final int SIM_ACTIVATION_NOTIFICATION_ID = 1;
    public static final int SWITCH_TO_REMOVABLE_SLOT_NOTIFICATION_ID = 2;

    /** Notification types */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                NotificationType.NETWORK_CONFIG,
                NotificationType.SWITCH_TO_REMOVABLE_SLOT,
                NotificationType.ENABLE_DSDS,
            })
    public @interface NotificationType {
        // The notification to remind users to config network Settings.
        int NETWORK_CONFIG = 1;
        // The notification to notify users that the device is switched to the removable slot.
        int SWITCH_TO_REMOVABLE_SLOT = 2;
        // The notification to notify users that the device is capable of DSDS.
        int ENABLE_DSDS = 3;
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
        mNotificationManager.createNotificationChannel(
                new NotificationChannel(
                        SWITCH_SLOT_CHANNEL_ID,
                        mContext.getString(R.string.sim_switch_channel_id),
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
        SubscriptionInfo activeRemovableSub = getActiveRemovableSub();

        if (activeRemovableSub == null) {
            Log.e(TAG, "No removable subscriptions found. Do not show notification.");
            return;
        }

        CharSequence displayName =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(activeRemovableSub, mContext);
        String carrierName =
                TextUtils.isEmpty(displayName)
                        ? mContext.getString(R.string.sim_card_label)
                        : displayName.toString();
        String title =
                mContext.getString(
                        R.string.post_dsds_reboot_notification_title_with_carrier, carrierName);
        String text = mContext.getString(R.string.post_dsds_reboot_notification_text);
        Intent clickIntent = new Intent(mContext, Settings.MobileNetworkListActivity.class);
        TaskStackBuilder stackBuilder =
                TaskStackBuilder.create(mContext).addNextIntent(clickIntent);
        PendingIntent contentIntent =
                stackBuilder.getPendingIntent(
                        0 /* requestCode */,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder =
                new Notification.Builder(mContext, SIM_SETUP_CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setContentIntent(contentIntent)
                        .setSmallIcon(R.drawable.ic_sim_alert)
                        .setAutoCancel(true);
        mNotificationManager.notify(SIM_ACTIVATION_NOTIFICATION_ID, builder.build());
    }

    /** Sends a push notification for switching to the removable slot. */
    public void sendSwitchedToRemovableSlotNotification() {
        String carrierName = getActiveCarrierName();
        Intent clickIntent = new Intent(mContext, Settings.MobileNetworkListActivity.class);
        TaskStackBuilder stackBuilder =
                TaskStackBuilder.create(mContext).addNextIntent(clickIntent);
        PendingIntent contentIntent =
                stackBuilder.getPendingIntent(
                        0 /* requestCode */,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String titleText =
                TextUtils.isEmpty(carrierName)
                        ? mContext.getString(
                                R.string.switch_to_removable_notification_no_carrier_name)
                        : mContext.getString(
                                R.string.switch_to_removable_notification, carrierName);
        Notification.Builder builder =
                new Notification.Builder(mContext, SWITCH_SLOT_CHANNEL_ID)
                        .setContentTitle(titleText)
                        .setContentText(
                                mContext.getString(R.string.network_changed_notification_text))
                        .setContentIntent(contentIntent)
                        .setSmallIcon(R.drawable.ic_sim_alert)
                        .setColor(
                                mContext.getResources()
                                        .getColor(
                                                R.color.homepage_generic_icon_background,
                                                null /* theme */))
                        .setAutoCancel(true);
        mNotificationManager.notify(SWITCH_TO_REMOVABLE_SLOT_NOTIFICATION_ID, builder.build());
    }

    /** Sends a push notification for enabling DSDS. */
    public void sendEnableDsdsNotification() {
        Intent parentIntent = new Intent(mContext, Settings.MobileNetworkListActivity.class);

        Intent clickIntent = new Intent(mContext, DsdsDialogActivity.class);

        TaskStackBuilder stackBuilder =
                TaskStackBuilder.create(mContext)
                        .addNextIntentWithParentStack(parentIntent)
                        .addNextIntent(clickIntent);
        PendingIntent contentIntent =
                stackBuilder.getPendingIntent(
                        0 /* requestCode */,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder =
                new Notification.Builder(mContext, SIM_SETUP_CHANNEL_ID)
                        .setContentTitle(
                                mContext.getString(R.string.dsds_notification_after_suw_title))
                        .setContentText(
                                mContext.getString(R.string.dsds_notification_after_suw_text))
                        .setContentIntent(contentIntent)
                        .setSmallIcon(R.drawable.ic_sim_alert)
                        .setAutoCancel(true);
        mNotificationManager.notify(SIM_ACTIVATION_NOTIFICATION_ID, builder.build());
    }

    @Nullable
    private SubscriptionInfo getActiveRemovableSub() {
        SubscriptionManager subscriptionManager =
                mContext.getSystemService(SubscriptionManager.class);
        return SubscriptionUtil.getActiveSubscriptions(subscriptionManager).stream()
                .filter(sub -> !sub.isEmbedded())
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private String getActiveCarrierName() {
        CarrierConfigManager configManager = mContext.getSystemService(CarrierConfigManager.class);
        TelephonyManager telManager = mContext.getSystemService(TelephonyManager.class);
        String telName = telManager.getSimOperatorName();
        if (configManager != null && configManager.getConfig() != null) {
            boolean override =
                    configManager
                            .getConfig()
                            .getBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL);
            String configName =
                    configManager
                            .getConfig()
                            .getString(CarrierConfigManager.KEY_CARRIER_NAME_STRING);

            return override || TextUtils.isEmpty(telName) ? configName : telName;
        }
        return telName;
    }
}
