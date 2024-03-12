/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.widget.Switch;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.flags.Flags;
import com.android.settings.widget.SettingsMainSwitchBar;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingSwitchBarControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private Switch mSwitch;

    private SettingsMainSwitchBar mSwitchBar;
    private AudioSharingSwitchBarController mController;
    private AudioSharingSwitchBarController.OnSwitchBarChangedListener mListener;
    private boolean mOnSwitchBarChanged;

    @Before
    public void setUp() {
        mSwitchBar = new SettingsMainSwitchBar(mContext);
        mOnSwitchBarChanged = false;
        mListener = () -> mOnSwitchBarChanged = true;
        mController = new AudioSharingSwitchBarController(mContext, mSwitchBar, mListener);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void bluetoothOff_switchDisabled() {
        mContext.registerReceiver(
                mController.mReceiver,
                mController.mIntentFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        mContext.sendBroadcast(intent);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mSwitch).setEnabled(false);
        assertThat(mOnSwitchBarChanged).isTrue();
    }
}
