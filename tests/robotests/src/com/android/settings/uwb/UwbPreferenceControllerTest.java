/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.uwb;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.PackageManager;
import android.uwb.UwbManager;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Unit tests for UWB preference toggle. */
@RunWith(RobolectricTestRunner.class)
public class UwbPreferenceControllerTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private Context mContext;
    private PackageManager mPackageManager;
    private UwbPreferenceController mController;

    @Mock
    private UwbManager mUwbManager;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mPackageManager = spy(mContext.getPackageManager());
        mController = new UwbPreferenceController(mContext, "uwb_settings");
        mController.mUwbManager = mUwbManager;
    }

    @Test
    public void getAvailabilityStatus_uwbDisabled_shouldReturnDisabled() {
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(true).when(mPackageManager)
                .hasSystemFeature(PackageManager.FEATURE_UWB);
        mController.mAirplaneModeOn = true;

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_uwbShown_shouldReturnAvailable() {
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(true).when(mPackageManager)
                .hasSystemFeature(PackageManager.FEATURE_UWB);
        mController.mAirplaneModeOn = false;

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_uwbNotShown_shouldReturnUnsupported() {
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(false).when(mPackageManager)
                .hasSystemFeature(PackageManager.FEATURE_UWB);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_uwbEnabled_shouldReturnTrue() {
        doReturn(mController.STATE_ENABLED_ACTIVE).when(mUwbManager).getAdapterState();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_uwbDisabled_shouldReturnFalse() {
        doReturn(mController.STATE_DISABLED).when(mUwbManager).getAdapterState();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_uwbDisabled_shouldEnableUwb() {
        clearInvocations(mUwbManager);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(true).when(mPackageManager)
                .hasSystemFeature(PackageManager.FEATURE_UWB);

        mController.setChecked(true);

        verify(mUwbManager).setUwbEnabled(true);
        verify(mUwbManager, never()).setUwbEnabled(false);
    }

    @Test
    public void setChecked_uwbEnabled_shouldDisableUwb() {
        clearInvocations(mUwbManager);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(true).when(mPackageManager)
                .hasSystemFeature(PackageManager.FEATURE_UWB);

        mController.setChecked(false);

        verify(mUwbManager).setUwbEnabled(false);
        verify(mUwbManager, never()).setUwbEnabled(true);
    }
}

