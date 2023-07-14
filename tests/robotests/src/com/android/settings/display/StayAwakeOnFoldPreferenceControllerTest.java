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

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class StayAwakeOnFoldPreferenceControllerTest {

    @Mock
    private Resources mResources;
    private Context mContext;
    private StayAwakeOnFoldPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mResources = Mockito.mock(Resources.class);
        mController = new StayAwakeOnFoldPreferenceController(mContext, "key", mResources);
    }

    @Test
    public void getAvailabilityStatus_withConfigNoShow_returnUnsupported() {
        when(mResources.getBoolean(R.bool.config_stay_awake_on_fold)).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_withConfigNoShow_returnAvailable() {
        when(mResources.getBoolean(R.bool.config_stay_awake_on_fold)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setChecked_enableStayAwakeOnFold_setChecked() {
        mController.setChecked(true);

        assertThat(isStayAwakeOnFoldEnabled())
                .isTrue();
    }

    @Test
    public void setChecked_disableStayAwakeOnFold_setUnchecked() {
        mController.setChecked(false);

        assertThat(isStayAwakeOnFoldEnabled())
                .isFalse();
    }

    @Test
    public void isChecked_enableStayAwakeOnFold_returnTrue() {
        enableStayAwakeOnFoldPreference();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disableStayAwakeOnFold_returnFalse() {
        disableStayAwakeOnFoldPreference();

        assertThat(mController.isChecked()).isFalse();
    }

    private void enableStayAwakeOnFoldPreference() {
        Settings.System.putInt(
                mContext.getContentResolver(),
                Settings.System.STAY_AWAKE_ON_FOLD,
                1);
    }

    private void disableStayAwakeOnFoldPreference() {
        Settings.System.putInt(
                mContext.getContentResolver(),
                Settings.System.STAY_AWAKE_ON_FOLD,
                0);
    }

    private boolean isStayAwakeOnFoldEnabled() {
        return (Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.STAY_AWAKE_ON_FOLD,
                0) == 1);
    }
}
