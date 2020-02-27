/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;

import com.android.settings.widget.MasterSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class AllInOneTetherPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private MasterSwitchPreference mPreference;
    @Mock
    private SharedPreferences mSharedPreferences;

    private AllInOneTetherPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = spy(AllInOneTetherPreferenceController.class);
        ReflectionHelpers.setField(mController, "mContext", mContext);
        ReflectionHelpers.setField(mController, "mBluetoothAdapter", mBluetoothAdapter);
        ReflectionHelpers.setField(mController, "mPreference", mPreference);
        ReflectionHelpers
            .setField(mController, "mTetherEnablerSharedPreferences", mSharedPreferences);
    }

    @Test
    public void onCreate_shouldInitBluetoothPan() {
        when(mBluetoothAdapter.getState()).thenReturn(BluetoothAdapter.STATE_ON);
        mController.onCreate();

        verify(mBluetoothAdapter).getState();
        verify(mBluetoothAdapter).getProfileProxy(mContext, mController.mBtProfileServiceListener,
                BluetoothProfile.PAN);
    }

    @Test
    public void onCreate_shouldNotInitBluetoothPanWhenBluetoothOff() {
        when(mBluetoothAdapter.getState()).thenReturn(BluetoothAdapter.STATE_OFF);
        mController.onCreate();

        verify(mBluetoothAdapter).getState();
        verifyNoMoreInteractions(mBluetoothAdapter);
    }

    @Test
    public void goThroughLifecycle_shouldDestroyBluetoothProfile() {
        final BluetoothPan pan = mock(BluetoothPan.class);
        final AtomicReference<BluetoothPan> panRef =
                ReflectionHelpers.getField(mController, "mBluetoothPan");
        panRef.set(pan);

        mController.onDestroy();

        verify(mBluetoothAdapter).closeProfileProxy(BluetoothProfile.PAN, pan);
    }
}
