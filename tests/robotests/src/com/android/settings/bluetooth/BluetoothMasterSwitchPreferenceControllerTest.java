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

import android.app.Fragment;
import android.content.Context;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothMasterSwitchPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private MasterSwitchPreference mPreference;
    @Mock
    private RestrictionUtils mRestrictionUtils;
    @Mock
    private Fragment mFragment;
    @Mock
    private SettingsActivity mActivity;

    private Context mContext;
    private BluetoothMasterSwitchPreferenceController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        mController = new BluetoothMasterSwitchPreferenceController(
                mContext, mBluetoothManager, mRestrictionUtils, mFragment, mActivity);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onResume_shouldRegisterCallback() {
        mController.onResume();

        verify(mBluetoothManager.getEventManager()).registerCallback(any(BluetoothCallback.class));
    }

    @Test
    public void onPause_shouldUnregisterCallback() {
        mController.onPause();

        verify(mBluetoothManager.getEventManager()).unregisterCallback(
                any(BluetoothCallback.class));
    }

    @Test
    public void onStart_shouldRegisterPreferenceChangeListener() {
        mController.displayPreference(mScreen);
        mController.onStart();

        verify(mPreference).setOnPreferenceChangeListener(any(OnPreferenceChangeListener.class));
    }

    @Test
    public void onStop_shouldRegisterPreferenceChangeListener() {
        mController.displayPreference(mScreen);
        mController.onStart();

        mController.onStop();

        verify(mPreference).setOnPreferenceChangeListener(null);
    }

    @Test
    public void onSummaryUpdated_shouldUpdatePreferenceSummary() {
        mController.displayPreference(mScreen);

        mController.onSummaryChanged("test summary");

        verify(mPreference).setSummary("test summary");
    }
}
