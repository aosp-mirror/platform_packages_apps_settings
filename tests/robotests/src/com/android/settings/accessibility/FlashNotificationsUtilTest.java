/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import static android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE;
import static android.hardware.camera2.CameraCharacteristics.LENS_FACING;
import static android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.FlashNotificationsUtil.getColorDescriptionText;
import static com.android.settings.accessibility.FlashNotificationsUtil.getFlashNotificationsState;
import static com.android.settings.accessibility.FlashNotificationsUtil.getScreenColor;
import static com.android.settings.accessibility.FlashNotificationsUtil.isTorchAvailable;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;
import static org.robolectric.shadow.api.Shadow.extract;
import static org.robolectric.shadows.ShadowCameraCharacteristics.newCameraCharacteristics;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

@RunWith(RobolectricTestRunner.class)
public class FlashNotificationsUtilTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private CameraManager mCameraManager = mContext.getSystemService(CameraManager.class);

    private ShadowCameraManager mShadowCameraManager;
    private ContentResolver mContentResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowCameraManager = Shadows.shadowOf(mCameraManager);
        mContentResolver = mContext.getContentResolver();
    }

    @Test
    public void isTorchAvailable_nullCameraManager_assertFalse() {
        when(mContext.getSystemService(CameraManager.class)).thenReturn(null);
        assertThat(isTorchAvailable(mContext)).isFalse();
    }

    @Test
    public void isTorchAvailable_noCamera_assertFalse() {
        assertThat(isTorchAvailable(mContext)).isFalse();
    }

    @Test
    public void isTorchAvailable_getCameraIdListThrowException_assertFalse()
            throws CameraAccessException {
        when(mCameraManager.getCameraIdList()).thenThrow(CameraAccessException.class);
        assertThat(isTorchAvailable(mContext)).isFalse();
    }

    @Test
    public void isTorchAvailable_getCameraCharacteristicsThrowException_assertFalse()
            throws CameraAccessException {
        CameraCharacteristics cameraCharacteristics = newCameraCharacteristics();
        mShadowCameraManager.addCamera("0", cameraCharacteristics);

        when(mCameraManager.getCameraCharacteristics("0")).thenThrow(CameraAccessException.class);

        assertThat(isTorchAvailable(mContext)).isFalse();
    }

    @Test
    public void isTorchAvailable_torchNotPresent_assertFalse() {
        setTorchNotPresent();

        assertThat(isTorchAvailable(mContext)).isFalse();
    }

    @Test
    public void isTorchAvailable_torchPresent_assertTrue() {
        setTorchPresent();

        assertThat(isTorchAvailable(mContext)).isTrue();
    }

    @Test
    public void isTorchAvailable_lensFacingFront_assertFalse() {
        CameraCharacteristics cameraCharacteristics = newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCameraCharacteristics = extract(cameraCharacteristics);
        shadowCameraCharacteristics.set(FLASH_INFO_AVAILABLE, true);
        shadowCameraCharacteristics.set(LENS_FACING, LENS_FACING_FRONT);
        mShadowCameraManager.addCamera("0", cameraCharacteristics);

        assertThat(isTorchAvailable(mContext)).isFalse();
    }

    @Test
    public void getScreenColor_undefinedColor_throwException() {
        assertThrows(FlashNotificationsUtil.ScreenColorNotFoundException.class, () ->
                getScreenColor(0x4D0000FF));
    }

    @Test
    public void getScreenColor_azureColor_returnAzure() throws Exception {
        assertThat(getScreenColor(0x660080FF)).isEqualTo(ScreenFlashNotificationColor.AZURE);
    }

    @Test
    public void getColorDescriptionText_undefinedColor_returnEmpty() {
        assertThat(getColorDescriptionText(mContext, 0x4D0000FF)).isEqualTo("");
    }

    @Test
    public void getColorDescriptionText_azureColor_returnAzureName() {
        assertThat(getColorDescriptionText(mContext, ScreenFlashNotificationColor.AZURE.mColorInt))
                .isEqualTo(mContext.getString(ScreenFlashNotificationColor.AZURE.mStringRes));
    }

    @Test
    public void getFlashNotificationsState_torchPresent_cameraOff_screenOff_assertOff() {
        setTorchPresent();
        Settings.System.putInt(mContentResolver, Settings.System.CAMERA_FLASH_NOTIFICATION, OFF);
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_FLASH_NOTIFICATION, OFF);

        assertThat(getFlashNotificationsState(mContext))
                .isEqualTo(FlashNotificationsUtil.State.OFF);
    }

    @Test
    public void getFlashNotificationsState_torchNotPresent_cameraOn_screenOff_assertOff() {
        setTorchNotPresent();
        Settings.System.putInt(mContentResolver, Settings.System.CAMERA_FLASH_NOTIFICATION, ON);
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_FLASH_NOTIFICATION, OFF);

        assertThat(getFlashNotificationsState(mContext))
                .isEqualTo(FlashNotificationsUtil.State.OFF);
    }

    @Test
    public void getFlashNotificationsState_torchPresent_cameraOn_screenOff_assertCamera() {
        setTorchPresent();
        Settings.System.putInt(mContentResolver, Settings.System.CAMERA_FLASH_NOTIFICATION, ON);
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_FLASH_NOTIFICATION, OFF);

        assertThat(getFlashNotificationsState(mContext))
                .isEqualTo(FlashNotificationsUtil.State.CAMERA);
    }

    @Test
    public void getFlashNotificationsState_torchPresent_cameraOff_screenOn_assertScreen() {
        setTorchPresent();
        Settings.System.putInt(mContentResolver, Settings.System.CAMERA_FLASH_NOTIFICATION, OFF);
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_FLASH_NOTIFICATION, ON);

        assertThat(getFlashNotificationsState(mContext))
                .isEqualTo(FlashNotificationsUtil.State.SCREEN);
    }

    @Test
    public void testGetFlashNotificationsState_torchPresent_cameraOn_screenOn_assertCameraScreen() {
        setTorchPresent();
        Settings.System.putInt(mContentResolver, Settings.System.CAMERA_FLASH_NOTIFICATION, ON);
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_FLASH_NOTIFICATION, ON);

        assertThat(getFlashNotificationsState(mContext))
                .isEqualTo(FlashNotificationsUtil.State.CAMERA_SCREEN);
    }

    private void setTorchPresent() {
        CameraCharacteristics cameraCharacteristics = newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCameraCharacteristics = extract(cameraCharacteristics);
        shadowCameraCharacteristics.set(FLASH_INFO_AVAILABLE, true);
        shadowCameraCharacteristics.set(LENS_FACING, LENS_FACING_BACK);
        mShadowCameraManager.addCamera("0", cameraCharacteristics);
    }

    private void setTorchNotPresent() {
        CameraCharacteristics cameraCharacteristics = newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCameraCharacteristics = extract(cameraCharacteristics);
        shadowCameraCharacteristics.set(FLASH_INFO_AVAILABLE, false);
        mShadowCameraManager.addCamera("0", cameraCharacteristics);
    }
}
