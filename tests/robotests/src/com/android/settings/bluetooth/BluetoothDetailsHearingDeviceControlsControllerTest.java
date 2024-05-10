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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;

import com.android.settings.SettingsActivity;
import com.android.settings.accessibility.AccessibilityHearingAidsFragment;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link BluetoothDetailsHearingDeviceControlsController}. */
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsHearingDeviceControlsControllerTest extends
        BluetoothDetailsControllerTestBase {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Captor
    private ArgumentCaptor<Intent> mIntentArgumentCaptor;
    private BluetoothDetailsHearingDeviceControlsController mController;

    @Override
    public void setUp() {
        super.setUp();

        FakeFeatureFactory.setupForTest();
        mController = new BluetoothDetailsHearingDeviceControlsController(mActivity, mFragment,
                mCachedDevice, mLifecycle);
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);
    }

    @Test
    public void isAvailable_isHearingAidDevice_available() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_isNotHearingAidDevice_notAvailable() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceClick_hearingDeviceControlsKey_LaunchExpectedFragment() {
        final Preference hearingControlsKeyPreference = new Preference(mContext);
        hearingControlsKeyPreference.setKey(
                BluetoothDetailsHearingDeviceControlsController.KEY_HEARING_DEVICE_CONTROLS);

        mController.onPreferenceClick(hearingControlsKeyPreference);

        assertStartActivityWithExpectedFragment(mActivity,
                AccessibilityHearingAidsFragment.class.getName());
    }

    private void assertStartActivityWithExpectedFragment(Context mockContext, String fragmentName) {
        verify(mockContext).startActivity(mIntentArgumentCaptor.capture());
        assertThat(mIntentArgumentCaptor.getValue()
                .getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(fragmentName);
    }
}
