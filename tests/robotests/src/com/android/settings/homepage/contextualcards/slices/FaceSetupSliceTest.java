/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.homepage.contextualcards.slices;

import static android.hardware.biometrics.BiometricConstants.BIOMETRIC_ERROR_NO_BIOMETRICS;
import static android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricConstants;
import android.hardware.biometrics.BiometricManager;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;


@RunWith(RobolectricTestRunner.class)
public class FaceSetupSliceTest {

    private BiometricManager mBiometricManager;
    private Context mContext;
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mContext = spy(RuntimeEnvironment.application);
        mPackageManager = spy(mContext.getPackageManager());
        mBiometricManager = spy(mContext.getSystemService(BiometricManager.class));
    }

    @Test
    public void getSlice_noFaceSupported_shouldReturnNull() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(false);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        final FaceSetupSlice setupSlice = new FaceSetupSlice(mContext);
        assertThat(setupSlice.getSlice()).isNull();
    }

    @Test
    public void getSlice_faceSupportedUserEnrolled_shouldReturnNull() {
        when(mBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(BiometricManager.class)).thenReturn(mBiometricManager);
        final FaceSetupSlice setupSlice = new FaceSetupSlice(mContext);
        assertThat(setupSlice.getSlice()).isNull();
    }

    @Test
    public void getSlice_faceSupportedUserNotEnrolled_shouldReturnNonNull() {
        when(mBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NO_BIOMETRICS);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(BiometricManager.class)).thenReturn(mBiometricManager);
        final FaceSetupSlice setupSlice = new FaceSetupSlice(mContext);
        assertThat(setupSlice.getSlice()).isNotNull();
    }
}