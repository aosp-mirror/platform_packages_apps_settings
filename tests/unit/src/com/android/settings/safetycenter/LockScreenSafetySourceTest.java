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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;
import android.safetycenter.SafetySourceStatus.IconAction;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.security.ScreenLockPreferenceDetailsUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class LockScreenSafetySourceTest {

    private static final String SUMMARY = "summary";
    private static final String FAKE_ACTION_CHOOSE_LOCK_GENERIC_FRAGMENT = "choose_lock_generic";
    private static final String FAKE_ACTION_SCREEN_LOCK_SETTINGS = "screen_lock_settings";

    private Context mApplicationContext;

    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Mock
    private ScreenLockPreferenceDetailsUtils mScreenLockPreferenceDetailsUtils;

    @Mock
    private LockPatternUtils mLockPatternUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplicationContext = ApplicationProvider.getApplicationContext();
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mApplicationContext))
                .thenReturn(mLockPatternUtils);
    }

    @After
    public void tearDown() {
        SafetyCenterManagerWrapper.sInstance = null;
    }

    @Test
    public void sendSafetyData_whenScreenLockIsEnabled_whenSafetyCenterIsDisabled_sendsNoData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isAvailable()).thenReturn(true);

        LockScreenSafetySource.sendSafetyData(mApplicationContext,
                mScreenLockPreferenceDetailsUtils);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_whenScreenLockIsDisabled_sendsNoData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isAvailable()).thenReturn(false);

        LockScreenSafetySource.sendSafetyData(mApplicationContext,
                mScreenLockPreferenceDetailsUtils);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_whenScreenLockIsEnabled_sendsData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.sendSafetyData(mApplicationContext,
                mScreenLockPreferenceDetailsUtils);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceData.getId()).isEqualTo(LockScreenSafetySource.SAFETY_SOURCE_ID);
        assertThat(safetySourceStatus.getTitle().toString())
                .isEqualTo(ResourcesUtils.getResourcesString(
                        mApplicationContext,
                        "unlock_set_unlock_launch_picker_title"));
        assertThat(safetySourceStatus.getSummary().toString())
                .isEqualTo(SUMMARY);
        assertThat(safetySourceStatus.getPendingIntent().getIntent()).isNotNull();
        assertThat(safetySourceStatus.getPendingIntent().getIntent().getAction())
                .isEqualTo(FAKE_ACTION_CHOOSE_LOCK_GENERIC_FRAGMENT);
    }

    @Test
    public void sendSafetyData_whenLockPatternIsSecure_sendsStatusLevelOk() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);

        LockScreenSafetySource.sendSafetyData(mApplicationContext,
                mScreenLockPreferenceDetailsUtils);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getStatusLevel())
                .isEqualTo(SafetySourceStatus.STATUS_LEVEL_OK);
    }

    @Test
    public void sendSafetyData_whenLockPatternIsNotSecure_sendsStatusLevelRecommendation() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);

        LockScreenSafetySource.sendSafetyData(mApplicationContext,
                mScreenLockPreferenceDetailsUtils);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getStatusLevel())
                .isEqualTo(SafetySourceStatus.STATUS_LEVEL_RECOMMENDATION);
    }

    @Test
    public void sendSafetyData_whenPasswordQualityIsManaged_sendsDisabled() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);

        LockScreenSafetySource.sendSafetyData(mApplicationContext,
                mScreenLockPreferenceDetailsUtils);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.isEnabled()).isFalse();
    }

    @Test
    public void sendSafetyData_whenPasswordQualityIsNotManaged_sendsEnabled() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);

        LockScreenSafetySource.sendSafetyData(mApplicationContext,
                mScreenLockPreferenceDetailsUtils);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.isEnabled()).isTrue();
    }

    @Test
    public void sendSafetyData_whenShouldShowGearMenu_sendsGearMenuActionIcon() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        final Intent launchScreenLockSettings = new Intent(FAKE_ACTION_SCREEN_LOCK_SETTINGS);
        when(mScreenLockPreferenceDetailsUtils.getLaunchScreenLockSettingsIntent())
                .thenReturn(launchScreenLockSettings);
        when(mScreenLockPreferenceDetailsUtils.shouldShowGearMenu()).thenReturn(true);

        LockScreenSafetySource.sendSafetyData(mApplicationContext,
                mScreenLockPreferenceDetailsUtils);

        final ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(
                SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        final IconAction iconAction = captor.getValue().getStatus().getIconAction();

        assertThat(iconAction.getIconType()).isEqualTo(IconAction.ICON_TYPE_GEAR);
        assertThat(iconAction.getPendingIntent().getIntent().getAction())
                .isEqualTo(FAKE_ACTION_SCREEN_LOCK_SETTINGS);
    }

    @Test
    public void sendSafetyData_whenShouldNotShowGearMenu_sendsNoGearMenuActionIcon() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.shouldShowGearMenu()).thenReturn(false);

        LockScreenSafetySource.sendSafetyData(mApplicationContext,
                mScreenLockPreferenceDetailsUtils);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getIconAction()).isNull();
    }

    @Test
    public void onLockScreenChange_whenSafetyCenterEnabled_sendsData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.onLockScreenChange(mApplicationContext);

        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void onLockScreenChange_whenSafetyCenterDisabled_sendsNoData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);

        LockScreenSafetySource.onLockScreenChange(mApplicationContext);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    private void whenScreenLockIsEnabled() {
        when(mScreenLockPreferenceDetailsUtils.isAvailable()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.getSummary(anyInt())).thenReturn(SUMMARY);

        Intent launchChooseLockGenericFragment = new Intent(
                FAKE_ACTION_CHOOSE_LOCK_GENERIC_FRAGMENT);
        when(mScreenLockPreferenceDetailsUtils.getLaunchChooseLockGenericFragmentIntent())
                .thenReturn(launchChooseLockGenericFragment);
    }
}
