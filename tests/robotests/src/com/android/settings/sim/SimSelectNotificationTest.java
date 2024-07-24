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
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DATA;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DISMISS;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_NAMES;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_WARNING_TYPE;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_WARNING_TYPE_DUAL_CDMA;
import static android.telephony.data.ApnSetting.TYPE_MMS;

import static com.android.settings.sim.SimDialogActivity.DATA_PICK;
import static com.android.settings.sim.SimDialogActivity.INVALID_PICK;
import static com.android.settings.sim.SimSelectNotification.ENABLE_MMS_NOTIFICATION_CHANNEL;
import static com.android.settings.sim.SimSelectNotification.ENABLE_MMS_NOTIFICATION_ID;
import static com.android.settings.sim.SimSelectNotification.SIM_WARNING_NOTIFICATION_CHANNEL;
import static com.android.settings.sim.SimSelectNotification.SIM_WARNING_NOTIFICATION_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
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
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class SimSelectNotificationTest {
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private Executor mExecutor;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private PackageManager mPackageManager;
    @Spy
    private Resources mResources = mContext.getResources();
    @Mock
    private SubscriptionInfo mSubInfo;
    @Mock
    private DisplayMetrics mDisplayMetrics;
    @Mock
    private SimDialogActivity mActivity;

    private final String mFakeDisplayName = "fake_display_name";
    private final CharSequence mFakeNotificationChannelTitle = "fake_notification_channel_title";
    private final CharSequence mFakeNotificationTitle = "fake_notification_title";
    private final String mFakeNotificationSummary = "fake_notification_Summary";

    // Dual CDMA combination notification.
    private final String mFakeDualCdmaWarningChannelTitle = "fake_dual_cdma_warning_channel_title";
    private final String mFakeDualCdmaWarningTitle = "fake_dual_cdma_warning_title";
    private final String mFakeDualCdmaWarningSummary = "fake_dual_cdma_warning_summary";
    private final String mSimCombinationName = " carrier1 & carrier 2";

    private int mSubId = 1;

    SimSelectNotification mSimSelectNotification = new SimSelectNotification();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.NOTIFICATION_SERVICE))
                .thenReturn(mNotificationManager);
        when(mContext.getSystemService(NotificationManager.class))
                .thenReturn(mNotificationManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE))
                .thenReturn(mSubscriptionManager);
        when(mContext.getApplicationInfo()).thenReturn(new ApplicationInfo());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.checkPermission(any(), any()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        when(mTelephonyManager.isDataEnabledForApn(TYPE_MMS)).thenReturn(false);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubInfo));
        SubscriptionUtil.setActiveSubscriptionsForTesting(Arrays.asList(mSubInfo));
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(true);
        when(mSubscriptionManager.getActiveSubscriptionInfo(mSubId)).thenReturn(mSubInfo);
        when(mSubInfo.getSubscriptionId()).thenReturn(mSubId);
        when(mSubInfo.getDisplayName()).thenReturn(mFakeDisplayName);
        when(mContext.getResources()).thenReturn(mResources);

        when(mResources.getBoolean(R.bool.config_show_sim_info)).thenReturn(true);
        when(mResources.getText(R.string.enable_sending_mms_notification_title))
                .thenReturn(mFakeNotificationTitle);
        when(mResources.getText(R.string.enable_mms_notification_channel_title))
                .thenReturn(mFakeNotificationChannelTitle);
        when(mResources.getString(R.string.enable_mms_notification_summary,
                mFakeDisplayName)).thenReturn(mFakeNotificationSummary);

        when(mResources.getText(R.string.dual_cdma_sim_warning_notification_channel_title))
                .thenReturn(mFakeDualCdmaWarningChannelTitle);
        when(mResources.getText(R.string.sim_combination_warning_notification_title))
                .thenReturn(mFakeDualCdmaWarningTitle);
        when(mResources.getString(R.string.dual_cdma_sim_warning_notification_summary,
                mSimCombinationName)).thenReturn(mFakeDualCdmaWarningSummary);

        when(mResources.getDisplayMetrics()).thenReturn(mDisplayMetrics);
        mDisplayMetrics.density = 1.5f;
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

    @Test
    public void onReceivePrimarySubListChange_NoExtra_notificationShouldNotSend() {
        Intent intent = new Intent(TelephonyManager.ACTION_PRIMARY_SUBSCRIPTION_LIST_CHANGED);

        // EXTRA_SUB_ID and EXTRA_ENABLE_MMS_DATA_REQUEST_REASON are required.
        mSimSelectNotification.onReceive(mContext, intent);
        verify(mNotificationManager, never()).createNotificationChannel(any());
    }

    @Test
    public void onReceivePrimarySubListChange_WithDataPickExtra_shouldStartActivity() {
        Intent intent = new Intent(TelephonyManager.ACTION_PRIMARY_SUBSCRIPTION_LIST_CHANGED);
        intent.putExtra(EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE,
                EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DATA);

        mSimSelectNotification.onReceive(mContext, intent);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        Intent capturedIntent = intentCaptor.getValue();
        assertThat(capturedIntent).isNotNull();
        assertThat(capturedIntent.getComponent().getClassName()).isEqualTo(
                SimDialogActivity.class.getName());
        assertThat(capturedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK)
                .isNotEqualTo(0);
        assertThat(capturedIntent.getIntExtra(SimDialogActivity.DIALOG_TYPE_KEY, INVALID_PICK))
                .isEqualTo(DATA_PICK);
    }

    @Test
    public void onReceivePrimarySubListChange_WithDismissExtra_shouldDismiss() {
        doReturn(mExecutor).when(mActivity).getMainExecutor();
        SimDialogProhibitService.supportDismiss(mActivity);

        Intent intent = new Intent(TelephonyManager.ACTION_PRIMARY_SUBSCRIPTION_LIST_CHANGED);
        intent.putExtra(EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE,
                EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DISMISS);

        mSimSelectNotification.onReceive(mContext, intent);
        clearInvocations(mContext);

        // Dismiss.
        verify(mExecutor).execute(any());
    }
    @Test
    public void onReceivePrimarySubListChange_DualCdmaWarning_notificationShouldSend() {
        Intent intent = new Intent(TelephonyManager.ACTION_PRIMARY_SUBSCRIPTION_LIST_CHANGED);

        intent.putExtra(EXTRA_SIM_COMBINATION_NAMES, mSimCombinationName);
        intent.putExtra(EXTRA_SIM_COMBINATION_WARNING_TYPE,
                EXTRA_SIM_COMBINATION_WARNING_TYPE_DUAL_CDMA);

        mSimSelectNotification.onReceive(mContext, intent);

        // Capture the notification channel created and verify its fields.
        ArgumentCaptor<NotificationChannel> nc = ArgumentCaptor.forClass(NotificationChannel.class);
        verify(mNotificationManager).createNotificationChannel(nc.capture());

        assertThat(nc.getValue().getId()).isEqualTo(SIM_WARNING_NOTIFICATION_CHANNEL);
        assertThat(nc.getValue().getName()).isEqualTo(mFakeDualCdmaWarningChannelTitle);
        assertThat(nc.getValue().getImportance()).isEqualTo(IMPORTANCE_HIGH);

        // Capture the notification it notifies and verify its fields.
        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(mNotificationManager).notify(
                eq(SIM_WARNING_NOTIFICATION_ID), notification.capture());
        assertThat(notification.getValue().extras.getCharSequence(Notification.EXTRA_TITLE))
                .isEqualTo(mFakeDualCdmaWarningTitle);
        assertThat(notification.getValue().extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
                .isEqualTo(mFakeDualCdmaWarningSummary);
        assertThat(notification.getValue().contentIntent).isNotNull();
    }
}
