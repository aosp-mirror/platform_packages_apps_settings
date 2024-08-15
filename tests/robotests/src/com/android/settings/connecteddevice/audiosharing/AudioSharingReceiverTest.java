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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.ACTION_LE_AUDIO_SHARING_STATE_CHANGE;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.BROADCAST_STATE_OFF;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.BROADCAST_STATE_ON;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_LE_AUDIO_SHARING_STATE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.R;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class AudioSharingReceiverTest {
    private static final String ACTION_LE_AUDIO_SHARING_STOP =
            "com.android.settings.action.BLUETOOTH_LE_AUDIO_SHARING_STOP";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ShadowApplication mShadowApplication;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private LocalBluetoothManager mLocalBluetoothManager;
    private FakeFeatureFactory mFeatureFactory;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private NotificationManager mNm;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
        mShadowApplication = Shadow.extract(mContext);
        mShadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void broadcastReceiver_isRegistered() {
        List<ShadowApplication.Wrapper> registeredReceivers =
                mShadowApplication.getRegisteredReceivers();

        int matchedCount =
                registeredReceivers.stream()
                        .filter(
                                receiver ->
                                        AudioSharingReceiver.class
                                                .getSimpleName()
                                                .equals(
                                                        receiver.broadcastReceiver
                                                                .getClass()
                                                                .getSimpleName()))
                        .collect(Collectors.toList())
                        .size();
        assertThat(matchedCount).isEqualTo(1);
    }

    @Test
    public void broadcastReceiver_receiveAudioSharingStateChangeIntentFlagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_LE_AUDIO_SHARING_STATE, BROADCAST_STATE_ON);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verifyNoInteractions(mNm);
    }

    @Test
    public void broadcastReceiver_receiveAudioSharingStateChangeIntentNoState_doNothing() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verifyNoInteractions(mNm);
    }

    @Test
    public void broadcastReceiver_receiveAudioSharingStateChangeIntentOnState_showNotification() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_LE_AUDIO_SHARING_STATE, BROADCAST_STATE_ON);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm).notify(eq(R.drawable.ic_bt_le_audio_sharing), any(Notification.class));
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_SHOW_AUDIO_SHARING_NOTIFICATION);
    }

    @Test
    public void
            broadcastReceiver_receiveAudioSharingStateChangeIntentOffState_cancelNotification() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_LE_AUDIO_SHARING_STATE, BROADCAST_STATE_OFF);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm).cancel(R.drawable.ic_bt_le_audio_sharing);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION);
    }

    @Test
    public void broadcastReceiver_receiveAudioSharingStopIntentFlagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STOP);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verifyNoInteractions(mBroadcast);
    }

    @Test
    public void
            broadcastReceiver_receiveAudioSharingStopIntent_notInBroadcast_cancelNotification() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        int broadcastId = 1;
        when(mBroadcast.getLatestBroadcastId()).thenReturn(broadcastId);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STOP);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mBroadcast, never()).stopBroadcast(broadcastId);
        verify(mNm).cancel(R.drawable.ic_bt_le_audio_sharing);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION);
    }

    @Test
    public void broadcastReceiver_receiveAudioSharingStopIntent_inBroadcast_stopBroadcast() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        int broadcastId = 1;
        when(mBroadcast.getLatestBroadcastId()).thenReturn(broadcastId);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STOP);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mBroadcast).stopBroadcast(broadcastId);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_STOP_AUDIO_SHARING_FROM_NOTIFICATION);
    }

    private AudioSharingReceiver getAudioSharingReceiver(Intent intent) {
        assertThat(mShadowApplication.hasReceiverForIntent(intent)).isTrue();
        List<BroadcastReceiver> receiversForIntent =
                mShadowApplication.getReceiversForIntent(intent);
        assertThat(receiversForIntent).hasSize(1);
        BroadcastReceiver broadcastReceiver = receiversForIntent.get(0);
        assertThat(broadcastReceiver).isInstanceOf(AudioSharingReceiver.class);
        return (AudioSharingReceiver) broadcastReceiver;
    }
}
