/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static android.provider.Settings.ENABLE_MMS_DATA_REQUEST_REASON_INCOMING_MMS;
import static android.provider.Settings.ENABLE_MMS_DATA_REQUEST_REASON_OUTGOING_MMS;
import static android.provider.Settings.EXTRA_ENABLE_MMS_DATA_REQUEST_REASON;
import static android.provider.Settings.EXTRA_SUB_ID;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_ALL;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DATA;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DISMISS;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_NONE;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_NAMES;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_WARNING_TYPE;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_WARNING_TYPE_DUAL_CDMA;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_WARNING_TYPE_NONE;
import static android.telephony.TelephonyManager.EXTRA_SUBSCRIPTION_ID;
import static android.telephony.data.ApnSetting.TYPE_MMS;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.HelpTrampoline;
import com.android.settings.R;
import com.android.settings.network.SatelliteRepository;
import com.android.settings.network.SubscriptionUtil;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimSelectNotification extends BroadcastReceiver {
    private static final String TAG = "SimSelectNotification";

    private static final int DEFAULT_TIMEOUT_MS = 1000;

    @VisibleForTesting
    public static final int SIM_SELECT_NOTIFICATION_ID = 1;
    @VisibleForTesting
    public static final int ENABLE_MMS_NOTIFICATION_ID = 2;
    @VisibleForTesting
    public static final int SIM_WARNING_NOTIFICATION_ID = 3;

    @VisibleForTesting
    public static final String SIM_SELECT_NOTIFICATION_CHANNEL =
            "sim_select_notification_channel";

    @VisibleForTesting
    public static final String ENABLE_MMS_NOTIFICATION_CHANNEL =
            "enable_mms_notification_channel";

    @VisibleForTesting
    public static final String SIM_WARNING_NOTIFICATION_CHANNEL =
            "sim_warning_notification_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SubscriptionUtil.isSimHardwareVisible(context)) {
            Log.w(TAG, "Received unexpected intent with null action.");
            return;
        }
        String action = intent.getAction();

        if (action == null) {
            Log.w(TAG, "Received unexpected intent with null action.");
            return;
        }

        switch (action) {
            case TelephonyManager.ACTION_PRIMARY_SUBSCRIPTION_LIST_CHANGED:
                PrimarySubscriptionListChangedService.scheduleJob(context, intent);
                break;
            case Settings.ACTION_ENABLE_MMS_DATA_REQUEST:
                onEnableMmsDataRequest(context, intent);
                break;
            default:
                Log.w(TAG, "Received unexpected intent " + intent.getAction());
        }
    }

    private void onEnableMmsDataRequest(Context context, Intent intent) {
        // Getting subId from extra.
        int subId = intent.getIntExtra(EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        if (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
            subId = SubscriptionManager.getDefaultSmsSubscriptionId();
        }

        SubscriptionManager subscriptionManager = ((SubscriptionManager) context.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE));
        if (!subscriptionManager.isActiveSubscriptionId(subId)) {
            Log.w(TAG, "onEnableMmsDataRequest invalid sub ID " + subId);
            return;
        }
        final SubscriptionInfo info = subscriptionManager.getActiveSubscriptionInfo(subId);
        if (info == null) {
            Log.w(TAG, "onEnableMmsDataRequest null SubscriptionInfo for sub ID " + subId);
            return;
        }

        // Getting request reason from extra, which will determine the notification title.
        CharSequence notificationTitle = null;
        int requestReason = intent.getIntExtra(EXTRA_ENABLE_MMS_DATA_REQUEST_REASON, -1);
        if (requestReason == ENABLE_MMS_DATA_REQUEST_REASON_INCOMING_MMS) {
            notificationTitle = context.getResources().getText(
                    R.string.enable_receiving_mms_notification_title);
        } else if (requestReason == ENABLE_MMS_DATA_REQUEST_REASON_OUTGOING_MMS) {
            notificationTitle = context.getResources().getText(
                    R.string.enable_sending_mms_notification_title);
        } else {
            Log.w(TAG, "onEnableMmsDataRequest invalid request reason " + requestReason);
            return;
        }

        TelephonyManager tm = ((TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE)).createForSubscriptionId(subId);

        if (tm.isDataEnabledForApn(TYPE_MMS)) {
            Log.w(TAG, "onEnableMmsDataRequest MMS data already enabled on sub ID " + subId);
            return;
        }

        CharSequence notificationSummary = context.getResources().getString(
                R.string.enable_mms_notification_summary,
                SubscriptionUtil.getUniqueSubscriptionDisplayName(info, context));

        cancelEnableMmsNotification(context);

        createEnableMmsNotification(context, notificationTitle, notificationSummary, subId);
    }

    /**
     * Handles changes to the primary subscription list, performing actions only
     * if the device is not currently in a satellite session. This method is
     * intended to be executed on a worker thread.
     *
     * @param context The application context
     * @param intent  The intent signaling a primary subscription change
     */
    @WorkerThread
    public static void onPrimarySubscriptionListChanged(@NonNull Context context,
            @NonNull Intent intent) {
        Log.d(TAG, "Checking satellite enabled status");
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<Boolean> isSatelliteSessionStartedFuture =
                new SatelliteRepository(context).requestIsSessionStarted(executor);
        boolean isSatelliteSessionStarted = false;
        try {
            isSatelliteSessionStarted =
                    isSatelliteSessionStartedFuture.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.w(TAG, "Can't get satellite session status", e);
        } finally {
            if (isSatelliteSessionStarted) {
                Log.i(TAG, "Device is in a satellite session.g Unable to handle primary"
                        + " subscription list changes");
            } else {
                Log.i(TAG, "Device is not in a satellite session. Handle primary"
                        + " subscription list changes");
                startSimSelectDialogIfNeeded(context, intent);
                sendSimCombinationWarningIfNeeded(context, intent);
            }
        }
    }

    private static void startSimSelectDialogIfNeeded(Context context, Intent intent) {
        int dialogType = intent.getIntExtra(EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE,
                EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_NONE);

        if (dialogType == EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_NONE) {
            return;
        }

        // Cancel any previous notifications
        cancelSimSelectNotification(context);

        // If the dialog type is to dismiss.
        if (dialogType == EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DISMISS) {
            SimDialogProhibitService.dismissDialog(context);
            return;
        }

        // Create a notification to tell the user that some defaults are missing
        createSimSelectNotification(context);

        if (dialogType == EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_ALL) {
            int subId = intent.getIntExtra(EXTRA_SUBSCRIPTION_ID,
                    SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
            int slotIndex = SubscriptionManager.getSlotIndex(subId);
            // If there is only one subscription, ask if user wants to use if for everything
            Intent newIntent = new Intent(context, SimDialogActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY,
                    SimDialogActivity.PREFERRED_PICK);
            newIntent.putExtra(SimDialogActivity.PREFERRED_SIM, slotIndex);
            context.startActivity(newIntent);
        } else if (dialogType == EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DATA) {
            // If there are multiple, ensure they pick default data
            Intent newIntent = new Intent(context, SimDialogActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.DATA_PICK);
            context.startActivity(newIntent);
        }
    }

    private static void sendSimCombinationWarningIfNeeded(Context context, Intent intent) {
        final int warningType = intent.getIntExtra(EXTRA_SIM_COMBINATION_WARNING_TYPE,
                EXTRA_SIM_COMBINATION_WARNING_TYPE_NONE);

        // Cancel any previous notifications
        cancelSimCombinationWarningNotification(context);

        if (warningType == EXTRA_SIM_COMBINATION_WARNING_TYPE_DUAL_CDMA) {
            // Create a notification to tell the user that there's a sim combination warning.
            createSimCombinationWarningNotification(context, intent);
        }
    }

    private static void createSimSelectNotification(Context context) {
        final Resources resources = context.getResources();

        NotificationChannel notificationChannel = new NotificationChannel(
                SIM_SELECT_NOTIFICATION_CHANNEL,
                resources.getText(R.string.sim_selection_channel_title),
                NotificationManager.IMPORTANCE_LOW);

        Notification.Builder builder =
                new Notification.Builder(context, SIM_SELECT_NOTIFICATION_CHANNEL)
                        .setSmallIcon(R.drawable.ic_sim_alert)
                        .setColor(context.getColor(R.color.sim_noitification))
                        .setContentTitle(resources.getText(R.string.sim_notification_title))
                        .setContentText(resources.getText(R.string.sim_notification_summary))
                        .setAutoCancel(true);
        Intent resultIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        resultIntent.setPackage(SETTINGS_PACKAGE_NAME);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.notify(SIM_SELECT_NOTIFICATION_ID, builder.build());
    }

    public static void cancelSimSelectNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(SIM_SELECT_NOTIFICATION_ID);
    }

    private void createEnableMmsNotification(Context context, CharSequence titleString,
            CharSequence notificationSummary, int subId) {
        final Resources resources = context.getResources();

        NotificationChannel notificationChannel = new NotificationChannel(
                ENABLE_MMS_NOTIFICATION_CHANNEL,
                resources.getText(R.string.enable_mms_notification_channel_title),
                NotificationManager.IMPORTANCE_HIGH);

        Notification.Builder builder =
                new Notification.Builder(context, ENABLE_MMS_NOTIFICATION_CHANNEL)
                        .setSmallIcon(R.drawable.ic_settings_24dp)
                        .setColor(context.getColor(R.color.sim_noitification))
                        .setContentTitle(titleString)
                        .setContentText(notificationSummary)
                        .setStyle(new Notification.BigTextStyle().bigText(notificationSummary))
                        .setAutoCancel(true);

        // Create the pending intent that will lead to the subscription setting page.
        Intent resultIntent = new Intent(Settings.ACTION_MMS_MESSAGE_SETTING);
        resultIntent.setPackage(SETTINGS_PACKAGE_NAME);
        resultIntent.putExtra(Settings.EXTRA_SUB_ID, subId);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(resultPendingIntent);

        // Notify the notification.
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.notify(ENABLE_MMS_NOTIFICATION_ID, builder.build());
    }

    private void cancelEnableMmsNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ENABLE_MMS_NOTIFICATION_ID);
    }

    private static void createSimCombinationWarningNotification(Context context, Intent intent) {
        final Resources resources = context.getResources();
        final String simNames = intent.getStringExtra(EXTRA_SIM_COMBINATION_NAMES);

        if (simNames == null) {
            return;
        }

        CharSequence dualCdmaSimWarningSummary = resources.getString(
                R.string.dual_cdma_sim_warning_notification_summary, simNames);

        NotificationChannel notificationChannel = new NotificationChannel(
                SIM_WARNING_NOTIFICATION_CHANNEL,
                resources.getText(R.string.dual_cdma_sim_warning_notification_channel_title),
                NotificationManager.IMPORTANCE_HIGH);

        Notification.Builder builder =
                new Notification.Builder(context, SIM_WARNING_NOTIFICATION_CHANNEL)
                        .setSmallIcon(R.drawable.ic_sim_alert)
                        .setColor(context.getColor(R.color.sim_noitification))
                        .setContentTitle(resources.getText(
                                R.string.sim_combination_warning_notification_title))
                        .setContentText(dualCdmaSimWarningSummary)
                        .setStyle(new Notification.BigTextStyle().bigText(
                                dualCdmaSimWarningSummary))
                        .setAutoCancel(true);

        // Create the pending intent that will lead to the helper page.
        Intent resultIntent = new Intent(context, HelpTrampoline.class);
        resultIntent.putExtra(Intent.EXTRA_TEXT, "help_uri_sim_combination_warning");

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.notify(SIM_WARNING_NOTIFICATION_ID, builder.build());
    }

    public static void cancelSimCombinationWarningNotification(Context context) {
        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.cancel(SIM_WARNING_NOTIFICATION_ID);
    }
}
