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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class StreamSettingsCategoryControllerTest {
    private static final String KEY = "audio_sharing_stream_settings_category";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private LocalBluetoothProfileManager mBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private PreferenceScreen mScreen;

    private StreamSettingsCategoryController mController;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private LocalBluetoothManager mLocalBluetoothManager;
    private Preference mPreference;

    @Before
    public void setUp() {
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mBtProfileManager);
        when(mBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mBtProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
        mController = new StreamSettingsCategoryController(mContext, KEY);
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(KEY)).thenReturn(mPreference);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void bluetoothOff_updateVisibility() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mContext.registerReceiver(
                mController.mReceiver,
                mController.mIntentFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isTrue();

        mShadowBluetoothAdapter.setEnabled(false);
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        mContext.sendBroadcast(intent);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_flagOn() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagOff() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void onStart_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mContext, times(0))
                .registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class), anyInt());
        verify(mBtProfileManager, times(0)).addServiceListener(mController);
    }

    @Test
    public void onStart_flagOn_registerCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mContext)
                .registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class), anyInt());
        verify(mBtProfileManager, times(0)).addServiceListener(mController);
    }

    @Test
    public void onStart_flagOnProfileNotReady_registerProfileManagerCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mController.onStart(mLifecycleOwner);
        verify(mContext)
                .registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class), anyInt());
        verify(mBtProfileManager).addServiceListener(mController);
    }

    @Test
    public void onStop_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mContext, times(0)).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mBtProfileManager, times(0)).removeServiceListener(mController);
    }

    @Test
    public void onStop_flagOn_unregisterCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        doNothing().when(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        mController.onStop(mLifecycleOwner);
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mBtProfileManager).removeServiceListener(mController);
    }

    @Test
    public void displayPreference_flagOff_preferenceInvisible() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mPreference.setVisible(true);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_BluetoothOff_preferenceInvisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mPreference.setVisible(true);
        mShadowBluetoothAdapter.setEnabled(false);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_BluetoothOnProfileNotReady_preferenceInvisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mPreference.setVisible(true);
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_BluetoothOnProfileReady_preferenceVisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mPreference.setVisible(false);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void onServiceConnected_updateVisibility() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();

        when(mBroadcast.isProfileReady()).thenReturn(true);
        mController.onServiceConnected();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isTrue();
    }
}
