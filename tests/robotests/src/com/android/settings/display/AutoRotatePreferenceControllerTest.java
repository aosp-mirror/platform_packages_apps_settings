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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import com.android.internal.view.RotationPolicy;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowRotationPolicy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class AutoRotatePreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    private SwitchPreference mPreference;
    private ContentResolver mContentResolver;
    private AutoRotatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mPreference = new SwitchPreference(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);

        mController = new AutoRotatePreferenceController(mContext, "auto_rotate");
    }

    @Test
    public void isAvailableWhenPolicyAllows() {
        assertThat(mController.isAvailable()).isFalse();

        enableAutoRotationPreference();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updatePreference_settingsIsOff_shouldTurnOffToggle() {
        disableAutoRotation();

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updatePreference_settingsIsOn_shouldTurnOnToggle() {
        enableAutoRotation();

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testGetAvailabilityStatus() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(BasePreferenceController
                .UNSUPPORTED_ON_DEVICE);

        enableAutoRotationPreference();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(BasePreferenceController
                .AVAILABLE);

        disableAutoRotationPreference();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(BasePreferenceController
                .UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testIsCheck() {
        assertThat(mController.isChecked()).isFalse();

        enableAutoRotation();

        assertThat(mController.isChecked()).isTrue();

        disableAutoRotation();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    @Config(shadows = {ShadowRotationPolicy.class})
    public void testSetCheck() {
        ShadowRotationPolicy.setRotationSupported(true);

        mController.setChecked(false);
        assertThat(mController.isChecked()).isFalse();
        assertThat(RotationPolicy.isRotationLocked(mContext)).isTrue();

        mController.setChecked(true);
        assertThat(mController.isChecked()).isTrue();
        assertThat(RotationPolicy.isRotationLocked(mContext)).isFalse();
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final AutoRotatePreferenceController controller =
                new AutoRotatePreferenceController(mContext, "auto_rotate");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final AutoRotatePreferenceController controller =
                new AutoRotatePreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }

    private void enableAutoRotationPreference() {
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mContext.getResources().getBoolean(anyInt())).thenReturn(true);
        Settings.System.putInt(mContentResolver,
                Settings.System.HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, 0);
    }

    private void disableAutoRotationPreference() {
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mContext.getResources().getBoolean(anyInt())).thenReturn(true);
        Settings.System.putInt(mContentResolver,
                Settings.System.HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, 1);
    }

    private void enableAutoRotation() {
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 1, UserHandle.USER_CURRENT);
    }

    private void disableAutoRotation() {
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0, UserHandle.USER_CURRENT);
    }
}
