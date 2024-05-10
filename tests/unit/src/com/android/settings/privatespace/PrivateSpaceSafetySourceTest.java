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

package com.android.settings.privatespace;


import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_DEVICE_REBOOTED;

import static com.android.settings.privatespace.PrivateSpaceSafetySource.SAFETY_SOURCE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Flags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.safetycenter.SafetyCenterManagerWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class PrivateSpaceSafetySourceTest {
    private static final SafetyEvent EVENT_TYPE_DEVICE_REBOOTED =
            new SafetyEvent.Builder(SAFETY_EVENT_TYPE_DEVICE_REBOOTED).build();
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    /** Required setup before a test. */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
    }

    /** Required setup after a test. */
    @After
    public void tearDown() {
        SafetyCenterManagerWrapper.sInstance = null;
    }

    /** Tests that when SC is disabled we don't set any data. */
    @Test
    public void onDeviceRebootedEvent_whenSafetyCenterDisabled_doesNotSetData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mContext)).thenReturn(false);

        PrivateSpaceSafetySource.setSafetySourceData(mContext, EVENT_TYPE_DEVICE_REBOOTED);

        verify(mSafetyCenterManagerWrapper, never()).setSafetySourceData(
                any(), any(), any(), any());
    }

    /** Tests that when SC is enabled we set data. */
    @Test
    public void onDeviceRebootedEvent_whenSafetyCenterEnabled_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mContext)).thenReturn(true);
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        PrivateSpaceSafetySource.setSafetySourceData(mContext, EVENT_TYPE_DEVICE_REBOOTED);

        verify(mSafetyCenterManagerWrapper).setSafetySourceData(
                any(), eq(SAFETY_SOURCE_ID), any(), eq(EVENT_TYPE_DEVICE_REBOOTED));
    }

    /** Tests that when the feature is disabled null data is set. */
    @Test
    public void setSafetySourceData_whenFeatureDisabled_setsNullData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mContext)).thenReturn(true);
        mSetFlagsRule.disableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        PrivateSpaceSafetySource.setSafetySourceData(mContext, EVENT_TYPE_DEVICE_REBOOTED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).setSafetySourceData(
                any(), eq(SAFETY_SOURCE_ID), captor.capture(), eq(EVENT_TYPE_DEVICE_REBOOTED));
        SafetySourceData safetySourceData = captor.getValue();
        assertThat(safetySourceData).isNull();
    }

    /** Tests that setSafetySourceData sets the source status enabled. */
    @Test
    public void setSafetySourceData_setsEnabled() {
        when(mSafetyCenterManagerWrapper.isEnabled(mContext)).thenReturn(true);
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        PrivateSpaceSafetySource.setSafetySourceData(mContext, EVENT_TYPE_DEVICE_REBOOTED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).setSafetySourceData(
                any(), eq(SAFETY_SOURCE_ID), captor.capture(), eq(EVENT_TYPE_DEVICE_REBOOTED));
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();
        assertThat(safetySourceStatus.isEnabled()).isTrue();
    }

    /** Tests that setSafetySourceData sets the PS settings page authenticator intent. */
    @Test
    public void setSafetySourceData_setsPsAuthenticatorIntent() {
        when(mSafetyCenterManagerWrapper.isEnabled(mContext)).thenReturn(true);
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        PrivateSpaceSafetySource.setSafetySourceData(mContext, EVENT_TYPE_DEVICE_REBOOTED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).setSafetySourceData(
                any(), eq(SAFETY_SOURCE_ID), captor.capture(), eq(EVENT_TYPE_DEVICE_REBOOTED));
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();
        assertThat(safetySourceStatus.getPendingIntent().getIntent().getComponent().getClassName())
                .isEqualTo(PrivateSpaceAuthenticationActivity.class.getName());
        assertThat(safetySourceStatus.getPendingIntent().getIntent().getIdentifier())
                .isEqualTo(SAFETY_SOURCE_ID);
    }
}
