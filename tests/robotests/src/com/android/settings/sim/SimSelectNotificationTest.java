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
package com.android.settings.sim;


import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.provider.Settings.ENABLE_MMS_DATA_REQUEST_REASON_INCOMING_MMS;
import static android.provider.Settings.ENABLE_MMS_DATA_REQUEST_REASON_OUTGOING_MMS;
import static android.provider.Settings.EXTRA_ENABLE_MMS_DATA_REQUEST_REASON;
import static android.provider.Settings.EXTRA_SUB_ID;
import static android.telephony.data.ApnSetting.TYPE_MMS;

import static com.android.settings.sim.SimSelectNotification.ENABLE_MMS_NOTIFICATION_CHANNEL;
import static com.android.settings.sim.SimSelectNotification.ENABLE_MMS_NOTIFICATION_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class SimSelectNotificationTest {
    @Mock
    private Context mContext;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Resources mResources;

    private String mFakeOperatorName = "fake_operator_name";
    private CharSequence mFakeNotificationChannelTitle = "fake_notification_channel_title";
    private CharSequence mFakeNotificationTitle = "fake_notification_title";
    private String mFakeNotificationSummary = "fake_notification_Summary";

    private int mSubId = 1;

    SimSelectNotification mSimSelectNotification = new SimSelectNotification();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.NOTIFICATION_SERVICE))
                .thenReturn(mNotificationManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mTelephonyManager);
        when(mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE))
                .thenReturn(mSubscriptionManager);
        when(mContext.getApplicationInfo()).thenReturn(new ApplicationInfo());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.checkPermission(any(), any()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getSimOperatorName()).thenReturn(mFakeOperatorName);
        when(mTelephonyManager.isDataEnabledForApn(TYPE_MMS)).thenReturn(false);
        when(mSubscriptionManager.isActiveSubId(mSubId)).thenReturn(true);
        when(mContext.getResources()).thenReturn(mResources);

        when(mResources.getText(R.string.enable_sending_mms_notification_title))
                .thenReturn(mFakeNotificationTitle);
        when(mResources.getText(R.string.enable_mms_notification_channel_title))
                .thenReturn(mFakeNotificationChannelTitle);
        when(mResources.getString(R.string.enable_mms_notification_summary,
                mFakeOperatorName)).thenReturn(mFakeNotificationSummary);
    }

    @Test
    public void onReceiveEnableMms_notificationShouldSend() {
        Intent intent = new Intent(Settings.ACTION_ENABLE_MMS_DATA_REQUEST);
        intent.putExtra(EXTRA_SUB_ID, mSubId);
        intent.putExtra(EXTRA_ENABLE_MMS_DATA_REQUEST_REASON,
                ENABLE_MMS_DATA_REQUEST_REASON_OUTGOING_MMS);

        mSimSelectNotification.onReceive(mContext, intent);

        // Capture the notification channel created and verify its fields.
        ArgumentCaptor<NotificationChannel> nc = ArgumentCaptor.forClass(NotificationChannel.class);
        verify(mNotificationManager).createNotificationChannel(nc.capture());

        assertThat(nc.getValue().getId()).isEqualTo(ENABLE_MMS_NOTIFICATION_CHANNEL);
        assertThat(nc.getValue().getName()).isEqualTo(mFakeNotificationChannelTitle);
        assertThat(nc.getValue().getImportance()).isEqualTo(IMPORTANCE_HIGH);

        // Capture the notification it notifies and verify its fields.
        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(mNotificationManager).notify(
                eq(ENABLE_MMS_NOTIFICATION_ID), notification.capture());
        assertThat(notification.getValue().extras.getCharSequence(Notification.EXTRA_TITLE))
                .isEqualTo(mFakeNotificationTitle);
        assertThat(notification.getValue().extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
                .isEqualTo(mFakeNotificationSummary);
        assertThat(notification.getValue().contentIntent).isNotNull();
    }

    @Test
    public void onReceiveEnableMms_NoExtra_notificationShouldNotSend() {
        Intent intent = new Intent(Settings.ACTION_ENABLE_MMS_DATA_REQUEST);

        // EXTRA_SUB_ID and EXTRA_ENABLE_MMS_DATA_REQUEST_REASON are required.
        mSimSelectNotification.onReceive(mContext, intent);
        verify(mNotificationManager, never()).createNotificationChannel(any());
    }

    @Test
    public void onReceiveEnableMms_MmsDataAlreadyEnabled_notificationShouldNotSend() {
        when(mTelephonyManager.isDataEnabledForApn(TYPE_MMS)).thenReturn(true);
        Intent intent = new Intent(Settings.ACTION_ENABLE_MMS_DATA_REQUEST);
        intent.putExtra(EXTRA_SUB_ID, mSubId);
        intent.putExtra(EXTRA_ENABLE_MMS_DATA_REQUEST_REASON,
                ENABLE_MMS_DATA_REQUEST_REASON_INCOMING_MMS);

        // If MMS data is already enabled, there's no need to trigger the notification.
        mSimSelectNotification.onReceive(mContext, intent);
        verify(mNotificationManager, never()).createNotificationChannel(any());
    }
}

