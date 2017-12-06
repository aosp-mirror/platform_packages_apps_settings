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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothDeviceRenamePreferenceControllerTest {

    private static final String DEVICE_NAME = "Nightshade";

    @Mock
    private LocalBluetoothAdapter mLocalAdapter;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Fragment mFragment;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private PreferenceScreen mScreen;
    private Context mContext;
    private Preference mPreference;
    private BluetoothDeviceRenamePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mPreference = new Preference(mContext);
        mPreference.setKey(BluetoothDeviceRenamePreferenceController.PREF_KEY);

        mController = new BluetoothDeviceRenamePreferenceController(
                mContext, mFragment, mLocalAdapter);
    }

    @Test
    public void testUpdateDeviceName_showSummaryWithDeviceName() {
        mController.updateDeviceName(mPreference, DEVICE_NAME);

        final CharSequence summary = mPreference.getSummary();

        assertThat(summary.toString()).isEqualTo(DEVICE_NAME);
    }

    @Test
    public void testHandlePreferenceTreeClick_startDialogFragment() {
        when(mFragment.getFragmentManager().beginTransaction()).thenReturn(mFragmentTransaction);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mFragmentTransaction).add(any(), anyString());
        verify(mFragmentTransaction).commit();
    }

    @Test
    public void displayPreference_shouldFindPreferenceWithMatchingPrefKey() {
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);

        mController.displayPreference(mScreen);

        assertThat(mController.mPreference.getKey()).isEqualTo(mController.getPreferenceKey());
    }
}
