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
 * limitations under the License.
 */

package com.android.settings.display;

import static android.provider.Settings.Secure.ADAPTIVE_SLEEP;

import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AdaptiveSleepPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "adaptive_sleep";

    private Context mContext;
    private AdaptiveSleepPreferenceController mController;
    private ContentResolver mContentResolver;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();

        doReturn(mPackageManager).when(mContext).getPackageManager();
        when(mPackageManager.getAttentionServicePackageName()).thenReturn("some.package");
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_GRANTED);

        mController = new AdaptiveSleepPreferenceController(mContext, PREFERENCE_KEY);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isControllerAvailable_ServiceUnavailable_returnUnsupported() {
        doReturn(null).when(mPackageManager).resolveService(isA(Intent.class), anyInt());

        assertThat(AdaptiveSleepPreferenceController.isControllerAvailable(mContext)).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void onPreferenceChange_turnOn_returnOn() {
        mController.onPreferenceChange(null, true);

        final int mode = Settings.Secure.getInt(mContentResolver, ADAPTIVE_SLEEP, 0);
        assertThat(mode).isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_turnOff_returnOff() {
        mController.onPreferenceChange(null, false);

        final int mode = Settings.Secure.getInt(mContentResolver, ADAPTIVE_SLEEP, 1);
        assertThat(mode).isEqualTo(0);
    }

    @Test
    public void setChecked_updatesCorrectly() {
        mController.setChecked(true);

        assertThat(mController.isChecked()).isTrue();

        mController.setChecked(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_no() {
        Settings.System.putInt(mContentResolver, ADAPTIVE_SLEEP, 0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_yes() {
        Settings.Secure.putInt(mContentResolver, ADAPTIVE_SLEEP, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void getSummary_settingOn_shouldReturnOnSummary() {
        mController.setChecked(true);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.adaptive_sleep_summary_on));
    }

    @Test
    public void getSummary_settingOff_shouldReturnOffSummary() {
        mController.setChecked(false);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.adaptive_sleep_summary_off));
    }

    @Test
    public void isSliceable_returnsTrue() {
        final AdaptiveSleepPreferenceController controller =
                new AdaptiveSleepPreferenceController(mContext, "any_key");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isChecked_returnsFalseWhenNotSufficientPermissions() {
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_DENIED);
        final AdaptiveSleepPreferenceController controller = new AdaptiveSleepPreferenceController(
                mContext, PREFERENCE_KEY);

        controller.setChecked(true);
        assertThat(controller.isChecked()).isFalse();
    }

    @Test
    public void isEnabled_returnsFalseWhenNotSufficientPermissions() {
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_DENIED);

        mController.setChecked(true);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isEnabled()).isFalse();
    }
}
