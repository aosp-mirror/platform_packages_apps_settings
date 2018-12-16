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
package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class DeviceModelPreferenceControllerTest {

    private final String KEY = "device_model";

    @Mock
    private Fragment mFragment;
    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private Context mContext;
    private DeviceModelPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new DeviceModelPreferenceController(mContext, KEY);
        mController.setHost(mFragment);
        mPreference = new Preference(mContext);
        mPreference.setKey(KEY);
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreference);
    }

    @Test
    public void isAvailable_returnTrueIfVisible() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_returnFalseIfNotVisible() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updatePreference_summaryShouldContainBuildModel() {
        mController.updateState(mPreference);

        assertThat(containBuildModel(mPreference.getSummary())).isTrue();
    }

    @Test
    public void clickPreference_shouldLaunchHardwareInfoDialog() {
        FragmentManager fragmentManager = mock(FragmentManager.class);
        when(mFragment.getFragmentManager()).thenReturn(fragmentManager);
        when(fragmentManager.beginTransaction()).thenReturn(mock(FragmentTransaction.class));

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
        verify(fragmentManager.beginTransaction())
                .add(any(HardwareInfoDialogFragment.class), eq(HardwareInfoDialogFragment.TAG));
    }

    @Test
    public void isSliceable_shouldBeTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    private boolean containBuildModel(CharSequence result) {
        final String oracle = mContext.getResources().getString(R.string.model_summary,
                Build.MODEL);
        return result.toString().contains(oracle);
    }
}
