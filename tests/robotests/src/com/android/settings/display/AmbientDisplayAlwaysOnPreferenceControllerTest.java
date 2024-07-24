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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.PowerManager;
import android.provider.Settings;

import com.android.internal.R;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSecureSettings.class)
public class AmbientDisplayAlwaysOnPreferenceControllerTest {

    private static final String TEST_PACKAGE = "com.android.test";

    @Mock
    private AmbientDisplayConfiguration mConfig;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PowerManager mPowerManager;
    @Mock
    private ApplicationInfo mApplicationInfo;

    private Context mContext;

    private ContentResolver mContentResolver;

    private AmbientDisplayAlwaysOnPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        mController = new AmbientDisplayAlwaysOnPreferenceController(mContext, "key");
        mController.setConfig(mConfig);

        mApplicationInfo.uid = 1;
        when(mContext.getString(R.string.config_systemWellbeing)).thenReturn(TEST_PACKAGE);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(
                TEST_PACKAGE, /* flag= */0);

        doReturn(mPowerManager).when(mContext).getSystemService(PowerManager.class);
        when(mPowerManager.isAmbientDisplaySuppressedForTokenByApp(anyString(), anyInt()))
                .thenReturn(false);
    }

    @Test
    public void getAvailabilityStatus_available() {
        when(mConfig.alwaysOnAvailableForUser(anyInt())).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                AmbientDisplayAlwaysOnPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_disabled_unsupported() {
        when(mConfig.alwaysOnAvailableForUser(anyInt())).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                AmbientDisplayAlwaysOnPreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_enabled() {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabled() {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_enabled() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ALWAYS_ON, -1))
                .isEqualTo(1);
    }

    @Test
    public void setChecked_disabled() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ALWAYS_ON, -1))
                .isEqualTo(0);
    }

    @Test
    public void isPublicSliceCorrectKey_returnsTrue() {
        final AmbientDisplayAlwaysOnPreferenceController controller =
                new AmbientDisplayAlwaysOnPreferenceController(mContext,
                        "ambient_display_always_on");
        assertThat(controller.isPublicSlice()).isTrue();
    }

    @Test
    public void isPublicSliceIncorrectKey_returnsFalse() {
        final AmbientDisplayAlwaysOnPreferenceController controller =
                new AmbientDisplayAlwaysOnPreferenceController(mContext, "bad_key");
        assertThat(controller.isPublicSlice()).isFalse();
    }

    @Test
    public void isSliceable_returnTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isAodSuppressedByBedtime_bedTimeModeOn_returnTrue() {
        when(mPowerManager.isAmbientDisplaySuppressedForTokenByApp(anyString(), anyInt()))
                .thenReturn(true);

        assertThat(AmbientDisplayAlwaysOnPreferenceController
                .isAodSuppressedByBedtime(mContext)).isTrue();
    }

    @Test
    public void isAodSuppressedByBedtime_bedTimeModeOff_returnFalse() {
        assertThat(AmbientDisplayAlwaysOnPreferenceController
                .isAodSuppressedByBedtime(mContext)).isFalse();
    }

    @Test
    public void isAodSuppressedByBedtime_notFoundWellbeingPackage_returnFalse()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getApplicationInfo(TEST_PACKAGE, /* flag= */0)).thenThrow(
                new PackageManager.NameNotFoundException());

        assertThat(AmbientDisplayAlwaysOnPreferenceController
                .isAodSuppressedByBedtime(mContext)).isFalse();
    }

    @Test
    public void getSummary_bedTimeModeOn_shouldReturnUnavailableSummary() {
        when(mPowerManager.isAmbientDisplaySuppressedForTokenByApp(anyString(), anyInt()))
                .thenReturn(true);

        final CharSequence summary = mController.getSummary();
        assertThat(summary).isEqualTo(mContext.getString(
                com.android.settings.R.string.aware_summary_when_bedtime_on));
    }
}
