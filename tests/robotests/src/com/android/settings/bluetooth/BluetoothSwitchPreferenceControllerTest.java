/*
 * Copyright (C) 2017 The Android Open Source Project
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class BluetoothSwitchPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private RestrictionUtils mRestrictionUtils;
    @Mock
    private LocalBluetoothAdapter mLocalBluetoothAdapter;

    private Context mContext;
    private BluetoothSwitchPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        FakeFeatureFactory.setupForTest();

        mController =
            new BluetoothSwitchPreferenceController(mContext, mBluetoothManager, mRestrictionUtils);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void testGetAvailabilityStatus_adapterNull_returnDisabled() {
        mController.mBluetoothAdapter = null;

        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.DISABLED_UNSUPPORTED);
    }

    @Test
    public void testGetAvailabilityStatus_adapterExisted_returnAvailable() {
        mController.mBluetoothAdapter = mLocalBluetoothAdapter;

        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void testOnStart_shouldRegisterPreferenceChangeListener() {
        mController.displayPreference(mScreen);
        mController.onStart();

        verify(mPreference).setOnPreferenceChangeListener(
                any(BluetoothSwitchPreferenceController.SwitchController.class));
    }

    @Test
    public void testOnStop_shouldRegisterPreferenceChangeListener() {
        mController.displayPreference(mScreen);
        mController.onStart();

        mController.onStop();

        verify(mPreference).setOnPreferenceChangeListener(null);
    }

    @Test
    public void testIsChecked_adapterNull_returnFalse() {
        mController.mBluetoothAdapter = null;

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testIsChecked_adapterExisted_returnFromAdapter() {
        mController.mBluetoothAdapter = mLocalBluetoothAdapter;
        doReturn(true).when(mLocalBluetoothAdapter).isEnabled();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testSetChecked_adapterExisted() {
        mController.mBluetoothAdapter = mLocalBluetoothAdapter;

        mController.setChecked(true);

        verify(mLocalBluetoothAdapter).setBluetoothEnabled(true);
    }
}
