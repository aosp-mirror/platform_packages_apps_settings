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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class BluetoothKeyMissingReceiverTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ShadowApplication mShadowApplication;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private NotificationManager mNm;
    @Mock private BluetoothDevice mBluetoothDevice;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.getApplication());
        mShadowApplication = Shadow.extract(mContext);
        mShadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
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
                                        BluetoothKeyMissingReceiver.class
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
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_KEY_MISSING_DIALOG)
    public void broadcastReceiver_receiveKeyMissingIntentFlagOff_doNothing() {
        Intent intent = spy(new Intent(BluetoothDevice.ACTION_KEY_MISSING));
        when(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).thenReturn(mBluetoothDevice);
        BluetoothKeyMissingReceiver bluetoothKeyMissingReceiver = getReceiver(intent);
        bluetoothKeyMissingReceiver.onReceive(mContext, intent);

        verifyNoInteractions(mNm);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_KEY_MISSING_DIALOG)
    public void broadcastReceiver_background_showNotification() {
        Intent intent = spy(new Intent(BluetoothDevice.ACTION_KEY_MISSING));
        when(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).thenReturn(mBluetoothDevice);
        BluetoothKeyMissingReceiver bluetoothKeyMissingReceiver = getReceiver(intent);
        bluetoothKeyMissingReceiver.onReceive(mContext, intent);

        verify(mNm).notify(eq(android.R.drawable.stat_sys_data_bluetooth), any(Notification.class));
        verify(mContext, never()).startActivityAsUser(any(), any());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_KEY_MISSING_DIALOG)
    public void broadcastReceiver_foreground_receiveKeyMissingIntent_showDialog() {
        when(mLocalBtManager.isForegroundActivity()).thenReturn(true);
        Intent intent = spy(new Intent(BluetoothDevice.ACTION_KEY_MISSING));
        when(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).thenReturn(mBluetoothDevice);
        BluetoothKeyMissingReceiver bluetoothKeyMissingReceiver = getReceiver(intent);
        bluetoothKeyMissingReceiver.onReceive(mContext, intent);

        verifyNoInteractions(mNm);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivityAsUser(captor.capture(), eq(UserHandle.CURRENT));
        assertThat(captor.getValue().getComponent().getClassName())
                .isEqualTo(BluetoothKeyMissingDialog.class.getName());
    }

    private BluetoothKeyMissingReceiver getReceiver(Intent intent) {
        assertThat(mShadowApplication.hasReceiverForIntent(intent)).isTrue();
        List<BroadcastReceiver> receiversForIntent =
                mShadowApplication.getReceiversForIntent(intent);
        assertThat(receiversForIntent).hasSize(1);
        BroadcastReceiver broadcastReceiver = receiversForIntent.get(0);
        assertThat(broadcastReceiver).isInstanceOf(BluetoothKeyMissingReceiver.class);
        return (BluetoothKeyMissingReceiver) broadcastReceiver;
    }
}
