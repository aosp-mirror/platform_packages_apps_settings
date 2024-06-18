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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsExtraOptionsControllerTest extends BluetoothDetailsControllerTestBase {

    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    @Mock private BluetoothDevice mBluetoothDevice;
    @Mock private Lifecycle mExtraOptionsLifecycle;
    @Mock private PreferenceCategory mOptionsContainer;
    @Mock private PreferenceScreen mPreferenceScreen;

    private BluetoothDetailsExtraOptionsController mController;
    private BluetoothFeatureProvider mFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mCachedDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mBluetoothDevice.getAnonymizedAddress()).thenReturn(MAC_ADDRESS);

        final FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFeatureProvider = fakeFeatureFactory.getBluetoothFeatureProvider();

        mController =
                new BluetoothDetailsExtraOptionsController(
                        mContext, mFragment, mCachedDevice, mExtraOptionsLifecycle);
    }

    @Test
    public void displayPreference_removeAndAddPreferences() {
        Preference preference1 = new SwitchPreferenceCompat(mContext);
        Preference preference2 = new SwitchPreferenceCompat(mContext);
        when(mFeatureProvider.getBluetoothExtraOptions(mContext, mCachedDevice))
                .thenReturn(ImmutableList.of(preference1, preference2));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mOptionsContainer);

        mController.displayPreference(mPreferenceScreen);
        ShadowLooper.idleMainLooper();

        verify(mOptionsContainer).removeAll();
        verify(mOptionsContainer).addPreference(preference1);
        verify(mOptionsContainer).addPreference(preference2);
    }
}
