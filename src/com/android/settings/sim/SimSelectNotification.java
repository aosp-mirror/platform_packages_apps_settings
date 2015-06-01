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

import com.android.internal.telephony.IccCardConstants;
import com.android.settings.R;
import com.android.settings.Settings.SimSettingsActivity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

public class SimSelectNotification extends BroadcastReceiver {
    private static final String TAG = "SimSelectNotification";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        final TelephonyManager telephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        final int numSlots = telephonyManager.getSimCount();
        final boolean isInProvisioning = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) == 0;

        // Do not create notifications on single SIM devices or when provisiong i.e. Setup Wizard.
        if (numSlots < 2 || isInProvisioning) {
            return;
        }

        // Cancel any previous notifications
        cancelNotification(context);

        // If sim state is not ABSENT or LOADED then ignore
        String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        if (!(IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simStatus) ||
                IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(simStatus))) {
            Log.d(TAG, "sim state is not Absent or Loaded");
            return;
        } else {
            Log.d(TAG, "simstatus = " + simStatus);
        }

        int state;
        for (int i = 0; i < numSlots; i++) {
            state = telephonyManager.getSimState(i);
            if (!(state == TelephonyManager.SIM_STATE_ABSENT
                    || state == TelephonyManager.SIM_STATE_READY
                    || state == TelephonyManager.SIM_STATE_UNKNOWN)) {
                Log.d(TAG, "All sims not in valid state yet");
                return;
            }
        }

        List<SubscriptionInfo> sil = subscriptionManager.getActiveSubscriptionInfoList();
        if (sil == null || sil.size() < 1) {
            Log.d(TAG, "Subscription list is empty");
            return;
        }

        // Clear defaults for any subscriptions which no longer exist
        subscriptionManager.clearDefaultsForInactiveSubIds();

        boolean dataSelected = SubscriptionManager.isUsableSubIdValue(
                SubscriptionManager.getDefaultDataSubId());
        boolean smsSelected = SubscriptionManager.isUsableSubIdValue(
                SubscriptionManager.getDefaultSmsSubId());

        // If data and sms defaults are selected, dont show notification (Calls default is optional)
        if (dataSelected && smsSelected) {
            Log.d(TAG, "Data & SMS default sims are selected. No notification");
            return;
        }

        // Create a notification to tell the user that some defaults are missing
        createNotification(context);

        if (sil.size() == 1) {
            // If there is only one subscription, ask if user wants to use if for everything
            Intent newIntent = new Intent(context, SimDialogActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.PREFERRED_PICK);
            newIntent.putExtra(SimDialogActivity.PREFERRED_SIM, sil.get(0).getSimSlotIndex());
            context.startActivity(newIntent);
        } else if (!dataSelected) {
            // If there are mulitple, ensure they pick default data
            Intent newIntent = new Intent(context, SimDialogActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.DATA_PICK);
            context.startActivity(newIntent);
        }
    }

    private void createNotification(Context context){
        final Resources resources = context.getResources();
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_sim_card_alert_white_48dp)
                .setColor(context.getColor(R.color.sim_noitification))
                .setContentTitle(resources.getString(R.string.sim_notification_title))
                .setContentText(resources.getString(R.string.sim_notification_summary));
        Intent resultIntent = new Intent(context, SimSettingsActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void cancelNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
