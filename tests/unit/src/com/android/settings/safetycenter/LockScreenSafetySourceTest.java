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

import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceIssue;
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
    private static final String FAKE_ACTION_OPEN_SUB_SETTING = "open_sub_setting";
    private static final String EXTRA_DESTINATION = "destination";
    private static final String FAKE_CHOOSE_LOCK_GENERIC_FRAGMENT = "choose_lock_generic";
    private static final String FAKE_SCREEN_LOCK_SETTINGS = "screen_lock_settings";
    private static final SafetyEvent EVENT_SOURCE_STATE_CHANGED =
            new SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build();

    private Context mApplicationContext;

    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Mock private ScreenLockPreferenceDetailsUtils mScreenLockPreferenceDetailsUtils;

    @Mock private LockPatternUtils mLockPatternUtils;

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
    public void setSafetySourceData_whenScreenLockEnabled_safetyCenterDisabled_doesNotSetData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isAvailable()).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    @Test
    public void setSafetySourceData_whenScreenLockIsDisabled_setsNullData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isAvailable()).thenReturn(false);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(LockScreenSafetySource.SAFETY_SOURCE_ID), eq(null), any());
    }

    @Test
    public void setSafetySourceData_setsDataForLockscreenSafetySource() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(LockScreenSafetySource.SAFETY_SOURCE_ID), any(), any());
    }

    @Test
    public void setSafetySourceData_setsDataWithCorrectSafetyEvent() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), any(), eq(EVENT_SOURCE_STATE_CHANGED));
    }

    @Test
    public void setSafetySourceData_whenScreenLockIsEnabled_setData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), captor.capture(), any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getTitle().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "unlock_set_unlock_launch_picker_title"));
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(SUMMARY);
        assertThat(safetySourceStatus.getPendingIntent().getIntent()).isNotNull();
        assertThat(safetySourceStatus.getPendingIntent().getIntent().getAction())
                .isEqualTo(FAKE_ACTION_OPEN_SUB_SETTING);
        assertThat(
                        safetySourceStatus
                                .getPendingIntent()
                                .getIntent()
                                .getStringExtra(EXTRA_DESTINATION))
                .isEqualTo(FAKE_CHOOSE_LOCK_GENERIC_FRAGMENT);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsNotMan_whenLockPattIsSec_setStatusLevelInfo() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_INFORMATION);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsNotMan_whenLockPattIsNotSec_setStatusLevelRec() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsMan_whenLockPattIsSec_setStatusLevelUnsp() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsMan_whenLockPattIsNotSec_setStatusLevelUnsp() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsNotMan_whenLockPattIsSec_doesNotSetIssues() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), captor.capture(), any());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getIssues()).isEmpty();
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsNotMan_whenLockPattIsNotSec_setsIssue() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getIssues()).hasSize(1);
        SafetySourceIssue issue = safetySourceData.getIssues().get(0);
        assertThat(issue.getId()).isEqualTo(LockScreenSafetySource.NO_SCREEN_LOCK_ISSUE_ID);
        assertThat(issue.getTitle().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_title"));
        assertThat(issue.getSummary().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_summary"));
        assertThat(issue.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION);
        assertThat(issue.getIssueTypeId())
                .isEqualTo(LockScreenSafetySource.NO_SCREEN_LOCK_ISSUE_TYPE_ID);
        assertThat(issue.getIssueCategory()).isEqualTo(SafetySourceIssue.ISSUE_CATEGORY_DEVICE);
        assertThat(issue.getActions()).hasSize(1);
        SafetySourceIssue.Action action = issue.getActions().get(0);
        assertThat(action.getId()).isEqualTo(LockScreenSafetySource.SET_SCREEN_LOCK_ACTION_ID);
        assertThat(action.getLabel().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_action_label"));
        assertThat(action.getPendingIntent().getIntent().getAction())
                .isEqualTo(FAKE_ACTION_OPEN_SUB_SETTING);
        assertThat(action.getPendingIntent().getIntent().getStringExtra(EXTRA_DESTINATION))
                .isEqualTo(FAKE_CHOOSE_LOCK_GENERIC_FRAGMENT);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsMan_whenLockPattIsSec_doesNotSetIssues() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), captor.capture(), any());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getIssues()).isEmpty();
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsMan_whenLockPattIsNotSec_doesNotSetIssues() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getIssues()).isEmpty();
    }

    @Test
    public void setSafetySourceData_whenPasswordQualityIsManaged_setDisabled() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.isEnabled()).isFalse();
        assertThat(safetySourceStatus.getPendingIntent()).isNull();
        assertThat(safetySourceStatus.getIconAction()).isNull();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);
        assertThat(safetySourceStatus.getSummary().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "disabled_by_policy_title"));
    }

    @Test
    public void setSafetySourceData_whenPasswordQualityIsNotManaged_setEnabled() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.shouldShowGearMenu()).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.isEnabled()).isTrue();
        assertThat(safetySourceStatus.getPendingIntent()).isNotNull();
        assertThat(safetySourceStatus.getIconAction()).isNotNull();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_INFORMATION);
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(SUMMARY);
    }

    @Test
    public void setSafetySourceData_whenShouldShowGearMenu_setGearMenuActionIcon() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.shouldShowGearMenu()).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        final ArgumentCaptor<SafetySourceData> captor =
                ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        final IconAction iconAction = captor.getValue().getStatus().getIconAction();

        assertThat(iconAction.getIconType()).isEqualTo(IconAction.ICON_TYPE_GEAR);
        assertThat(iconAction.getPendingIntent().getIntent().getAction())
                .isEqualTo(FAKE_ACTION_OPEN_SUB_SETTING);
        assertThat(iconAction.getPendingIntent().getIntent().getStringExtra(EXTRA_DESTINATION))
                .isEqualTo(FAKE_SCREEN_LOCK_SETTINGS);
    }

    @Test
    public void setSafetySourceData_whenShouldNotShowGearMenu_doesNotSetGearMenuActionIcon() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.shouldShowGearMenu()).thenReturn(false);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getIconAction()).isNull();
    }

    @Test
    public void onLockScreenChange_whenSafetyCenterEnabled_setsLockscreenAndBiometricData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.onLockScreenChange(mApplicationContext);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(LockScreenSafetySource.SAFETY_SOURCE_ID), any(), any());
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(BiometricsSafetySource.SAFETY_SOURCE_ID), any(), any());
    }

    @Test
    public void onLockScreenChange_whenSafetyCenterDisabled_doesNotSetData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);

        LockScreenSafetySource.onLockScreenChange(mApplicationContext);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    private void whenScreenLockIsEnabled() {
        when(mScreenLockPreferenceDetailsUtils.isAvailable()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.getSummary(anyInt())).thenReturn(SUMMARY);

        Intent launchChooseLockGenericFragment = new Intent(FAKE_ACTION_OPEN_SUB_SETTING);
        launchChooseLockGenericFragment.putExtra(
                EXTRA_DESTINATION, FAKE_CHOOSE_LOCK_GENERIC_FRAGMENT);
        when(mScreenLockPreferenceDetailsUtils.getLaunchChooseLockGenericFragmentIntent(anyInt()))
                .thenReturn(launchChooseLockGenericFragment);

        Intent launchScreenLockSettings = new Intent(FAKE_ACTION_OPEN_SUB_SETTING);
        launchScreenLockSettings.putExtra(EXTRA_DESTINATION, FAKE_SCREEN_LOCK_SETTINGS);
        when(mScreenLockPreferenceDetailsUtils.getLaunchScreenLockSettingsIntent(anyInt()))
                .thenReturn(launchScreenLockSettings);
    }
}
