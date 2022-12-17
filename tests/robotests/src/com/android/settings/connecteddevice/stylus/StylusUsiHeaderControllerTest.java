/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.connecteddevice.stylus;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.BatteryState;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class StylusUsiHeaderControllerTest {

    private Context mContext;
    private StylusUsiHeaderController mController;
    private LayoutPreference mLayoutPreference;
    private PreferenceScreen mScreen;
    private InputDevice mInputDevice;

    @Mock
    private InputManager mInputManager;
    @Mock
    private BatteryState mBatteryState;
    @Mock
    private Bundle mBundle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        InputDevice device = new InputDevice.Builder().setId(1).setSources(
                InputDevice.SOURCE_BLUETOOTH_STYLUS).build();
        mInputDevice = spy(device);
        when(mInputDevice.getBatteryState()).thenReturn(mBatteryState);
        when(mBatteryState.getCapacity()).thenReturn(1f);
        when(mBatteryState.isPresent()).thenReturn(true);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(InputManager.class)).thenReturn(mInputManager);
        mController = new StylusUsiHeaderController(mContext, mInputDevice);

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mLayoutPreference = new LayoutPreference(mContext,
                LayoutInflater.from(mContext).inflate(R.layout.advanced_bt_entity_header, null));
        mLayoutPreference.setKey(mController.getPreferenceKey());

        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mScreen.addPreference(mLayoutPreference);

    }

    @Test
    public void onCreate_registersBatteryListener() {
        mController.onCreate(mBundle);

        verify(mInputManager).addInputDeviceBatteryListener(mInputDevice.getId(),
                mContext.getMainExecutor(),
                mController);
    }

    @Test
    public void onDestroy_unregistersBatteryListener() {
        mController.onDestroy();

        verify(mInputManager).removeInputDeviceBatteryListener(mInputDevice.getId(),
                mController);
    }

    @Test
    public void displayPreference_showsCorrectTitle() {
        mController.displayPreference(mScreen);

        assertThat(((TextView) mLayoutPreference.findViewById(
                R.id.entity_header_title)).getText().toString()).isEqualTo(
                mContext.getString(R.string.stylus_usi_header_title));
    }

    @Test
    public void displayPreference_hasBattery_showsCorrectBatterySummary() {
        mController.displayPreference(mScreen);

        assertThat(mLayoutPreference.findViewById(
                R.id.entity_header_summary).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(((TextView) mLayoutPreference.findViewById(
                R.id.entity_header_summary)).getText().toString()).isEqualTo(
                "100%");
    }

    @Test
    public void displayPreference_noBattery_showsEmptySummary() {
        when(mBatteryState.isPresent()).thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mLayoutPreference.findViewById(
                R.id.entity_header_summary).getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void displayPreference_invalidCapacity_showsEmptySummary() {
        when(mBatteryState.getCapacity()).thenReturn(-1f);

        mController.displayPreference(mScreen);

        assertThat(mLayoutPreference.findViewById(
                R.id.entity_header_summary).getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onBatteryStateChanged_updatesSummary() {
        mController.displayPreference(mScreen);

        when(mBatteryState.getCapacity()).thenReturn(0.2f);
        mController.onBatteryStateChanged(mInputDevice.getId(),
                System.currentTimeMillis(), mBatteryState);

        assertThat(((TextView) mLayoutPreference.findViewById(
                R.id.entity_header_summary)).getText().toString()).isEqualTo(
                "20%");
    }
}
