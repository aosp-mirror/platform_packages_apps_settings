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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamMediaService.BROADCAST_ID;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamMediaService.DEVICES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.session.ISession;
import android.media.session.ISessionController;
import android.media.session.MediaSessionManager;
import android.os.RemoteException;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.DisplayMetrics;

import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowAudioStreamsHelper.class,
        })
public class AudioStreamMediaServiceTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock private Resources mResources;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    @Mock private AudioStreamsHelper mAudioStreamsHelper;
    @Mock private NotificationManager mNotificationManager;
    @Mock private MediaSessionManager mMediaSessionManager;
    @Mock private BluetoothEventManager mBluetoothEventManager;
    @Mock private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock private VolumeControlProfile mVolumeControlProfile;
    @Mock private BluetoothDevice mDevice;
    @Mock private ISession mISession;
    @Mock private ISessionController mISessionController;
    @Mock private PackageManager mPackageManager;
    @Mock private DisplayMetrics mDisplayMetrics;
    @Mock private Context mContext;
    private AudioStreamMediaService mAudioStreamMediaService;

    @Before
    public void setUp() {
        ShadowAudioStreamsHelper.setUseMock(mAudioStreamsHelper);
        when(mAudioStreamsHelper.getLeBroadcastAssistant()).thenReturn(mLeBroadcastAssistant);
        ShadowBluetoothAdapter shadowBluetoothAdapter =
                Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        when(mLocalBtManager.getEventManager()).thenReturn(mBluetoothEventManager);
        when(mLocalBtManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getVolumeControlProfile())
                .thenReturn(mVolumeControlProfile);

        mAudioStreamMediaService = spy(new AudioStreamMediaService());
        ReflectionHelpers.setField(mAudioStreamMediaService, "mBase", mContext);
        when(mAudioStreamMediaService.getSystemService(anyString()))
                .thenReturn(mMediaSessionManager);
        when(mMediaSessionManager.createSession(any(), anyString(), any())).thenReturn(mISession);
        try {
            when(mISession.getController()).thenReturn(mISessionController);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        doReturn(mNotificationManager)
                .when(mAudioStreamMediaService)
                .getSystemService(NotificationManager.class);
        when(mAudioStreamMediaService.getApplicationInfo()).thenReturn(new ApplicationInfo());
        when(mAudioStreamMediaService.getResources()).thenReturn(mResources);
        when(mAudioStreamMediaService.getPackageManager()).thenReturn(mPackageManager);
        when(mResources.getDisplayMetrics()).thenReturn(mDisplayMetrics);
        mDisplayMetrics.density = 1.5f;
    }

    @After
    public void tearDown() {
        mAudioStreamMediaService.stopSelf();
        ShadowBluetoothUtils.reset();
        ShadowAudioStreamsHelper.reset();
    }

    @Test
    public void onCreate_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);

        mAudioStreamMediaService.onCreate();

        verify(mNotificationManager, never()).createNotificationChannel(any());
        verify(mBluetoothEventManager, never()).registerCallback(any());
        verify(mLeBroadcastAssistant, never()).registerServiceCallBack(any(), any());
        verify(mVolumeControlProfile, never()).registerCallback(any(), any());
    }

    @Test
    public void onCreate_flagOn_init() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);

        mAudioStreamMediaService.onCreate();

        verify(mNotificationManager).createNotificationChannel(any());
        verify(mBluetoothEventManager).registerCallback(any());
        verify(mLeBroadcastAssistant).registerServiceCallBack(any(), any());
        verify(mVolumeControlProfile).registerCallback(any(), any());
    }

    @Test
    public void onDestroy_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);

        mAudioStreamMediaService.onCreate();
        mAudioStreamMediaService.onDestroy();

        verify(mBluetoothEventManager, never()).unregisterCallback(any());
        verify(mLeBroadcastAssistant, never()).unregisterServiceCallBack(any());
        verify(mVolumeControlProfile, never()).unregisterCallback(any());
    }

    @Test
    public void onDestroy_flagOn_cleanup() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);

        mAudioStreamMediaService.onCreate();
        mAudioStreamMediaService.onDestroy();

        verify(mBluetoothEventManager).unregisterCallback(any());
        verify(mLeBroadcastAssistant).unregisterServiceCallBack(any());
        verify(mVolumeControlProfile).unregisterCallback(any());
    }

    @Test
    public void onStartCommand_noBroadcastId_stopSelf() {
        mAudioStreamMediaService.onStartCommand(new Intent(), /* flags= */ 0, /* startId= */ 0);

        assertThat(mAudioStreamMediaService.mLocalSession).isNull();
        verify(mAudioStreamMediaService).stopSelf();
    }

    @Test
    public void onStartCommand_noDevice_stopSelf() {
        Intent intent = new Intent();
        intent.putExtra(BROADCAST_ID, 1);

        mAudioStreamMediaService.onStartCommand(intent, /* flags= */ 0, /* startId= */ 0);

        assertThat(mAudioStreamMediaService.mLocalSession).isNull();
        verify(mAudioStreamMediaService).stopSelf();
    }

    @Test
    public void onStartCommand_createSessionAndStartForeground() {
        var devices = new ArrayList<BluetoothDevice>();
        devices.add(mDevice);

        Intent intent = new Intent();
        intent.putExtra(BROADCAST_ID, 1);
        intent.putParcelableArrayListExtra(DEVICES, devices);

        mAudioStreamMediaService.onStartCommand(intent, /* flags= */ 0, /* startId= */ 0);

        assertThat(mAudioStreamMediaService.mLocalSession).isNotNull();
        verify(mAudioStreamMediaService, never()).stopSelf();

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(mAudioStreamMediaService).startForeground(anyInt(), notification.capture());
        assertThat(notification.getValue().getSmallIcon()).isNotNull();
        assertThat(notification.getValue().isStyle(Notification.MediaStyle.class)).isTrue();
    }
}
