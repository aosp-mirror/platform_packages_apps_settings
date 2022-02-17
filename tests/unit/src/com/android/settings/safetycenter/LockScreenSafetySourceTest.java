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

package com.android.settings.safetycenter;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.SettingsActivity;
import com.android.settings.password.ChooseLockGeneric;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class LockScreenSafetySourceTest {

    private Context mApplicationContext;

    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Mock
    private SafetyCenterStatusHolder mSafetyCenterStatusHolder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplicationContext = ApplicationProvider.getApplicationContext();
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
        SafetyCenterStatusHolder.sInstance = mSafetyCenterStatusHolder;
    }

    @After
    public void tearDown() {
        SafetyCenterManagerWrapper.sInstance = null;
        SafetyCenterStatusHolder.sInstance = null;
    }

    @Test
    public void sendSafetyData_whenSafetyCenterIsDisabled_sendsNoData() {
        when(mSafetyCenterStatusHolder.isEnabled(mApplicationContext)).thenReturn(false);

        LockScreenSafetySource.sendSafetyData(mApplicationContext);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_whenSafetyCenterIsEnabled_sendsPlaceholderData() {
        when(mSafetyCenterStatusHolder.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.sendSafetyData(mApplicationContext);
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceData.getId()).isEqualTo(LockScreenSafetySource.SAFETY_SOURCE_ID);
        assertThat(safetySourceStatus.getTitle().toString()).isEqualTo("Lock Screen");
        assertThat(safetySourceStatus.getSummary().toString())
                .isEqualTo("Lock screen settings");
        assertThat(safetySourceStatus.getStatusLevel())
                .isEqualTo(SafetySourceStatus.STATUS_LEVEL_OK);
        assertThat(safetySourceStatus.getPendingIntent()).isNotNull();
        assertThat(safetySourceStatus.getPendingIntent().getIntent().getStringExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ChooseLockGeneric.ChooseLockGenericFragment.class.getName());
    }
}
