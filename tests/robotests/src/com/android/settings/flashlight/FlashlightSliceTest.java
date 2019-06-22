/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */

package com.android.settings.flashlight;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.provider.Settings;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FlashlightSliceTest {

    private Context mContext;
    private ShadowCameraManager mShadowCameraManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowCameraManager = Shadows.shadowOf(mContext.getSystemService(CameraManager.class));

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void getFlashlightSlice_correctData() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), Settings.Secure.FLASHLIGHT_AVAILABLE, 1);

        final Slice slice = new FlashlightSlice(mContext).getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.power_flashlight));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);
    }

    @Test
    public void isFlashlightAvailable_nullSecureAttr_noFlashUnit_returnFalse() {
        assertThat(FlashlightSlice.isFlashlightAvailable(mContext)).isFalse();
    }

    @Test
    public void isFlashlightAvailable_nullSecureAttr_hasFlashUnit_returnTrue() {
        final CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        final ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);
        shadowCharacteristics.set(CameraCharacteristics.FLASH_INFO_AVAILABLE, true);
        shadowCharacteristics
                .set(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK);
        mShadowCameraManager.addCamera("camera_id", characteristics);

        assertThat(FlashlightSlice.isFlashlightAvailable(mContext)).isTrue();
    }
}
