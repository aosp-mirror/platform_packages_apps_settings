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

package com.android.settings.notification;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.notification.AssistantCapabilityPreferenceController.PRIORITIZER_KEY;
import static com.android.settings.notification.AssistantCapabilityPreferenceController.RANKING_KEY;
import static com.android.settings.notification.AssistantCapabilityPreferenceController.SMART_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.service.notification.Adjustment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class AssistantCapabilityPreferenceControllerTest {

    @Mock
    private NotificationBackend mBackend;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private AssistantCapabilityPreferenceController mPrioritizerController;
    private AssistantCapabilityPreferenceController mRankingController;
    private AssistantCapabilityPreferenceController mChipController;
    private Preference mPrioritizerPreference;
    private Preference mRankingPreference;
    private Preference mChipPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPrioritizerController = new AssistantCapabilityPreferenceController(
                mContext, PRIORITIZER_KEY);
        mPrioritizerController.setBackend(mBackend);
        mPrioritizerPreference = new Preference(mContext);
        mPrioritizerPreference.setKey(mPrioritizerController.getPreferenceKey());
        when(mScreen.findPreference(
                mPrioritizerController.getPreferenceKey())).thenReturn(mPrioritizerPreference);
        mRankingController = new AssistantCapabilityPreferenceController(
                mContext, RANKING_KEY);
        mRankingController.setBackend(mBackend);
        mRankingPreference = new Preference(mContext);
        mRankingPreference.setKey(mRankingController.getPreferenceKey());
        when(mScreen.findPreference(
                mRankingController.getPreferenceKey())).thenReturn(mRankingPreference);
        mChipController = new AssistantCapabilityPreferenceController(mContext, SMART_KEY);
        mChipController.setBackend(mBackend);
        mChipPreference = new Preference(mContext);
        mChipPreference.setKey(mChipController.getPreferenceKey());
        when(mScreen.findPreference(
                mChipController.getPreferenceKey())).thenReturn(mChipPreference);
    }

    @Test
    public void getAvailabilityStatus_NAS() {
        when(mBackend.getAllowedNotificationAssistant()).thenReturn(mock(ComponentName.class));
        assertThat(mPrioritizerController.getAvailabilityStatus())
                .isEqualTo(AVAILABLE);
        assertThat(mChipController.getAvailabilityStatus())
                .isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noNAS() {
        when(mBackend.getAllowedNotificationAssistant()).thenReturn(null);
        assertThat(mPrioritizerController.getAvailabilityStatus())
                .isEqualTo(DISABLED_DEPENDENT_SETTING);
        assertThat(mChipController.getAvailabilityStatus())
                .isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void isChecked_prioritizerSettingIsOff_false() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_USER_SENTIMENT);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mPrioritizerController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_prioritizerSettingIsOn_true() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_IMPORTANCE);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mPrioritizerController.isChecked()).isTrue();

        capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_RANKING_SCORE);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mPrioritizerController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_rankingSettingIsOff_false() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_IMPORTANCE);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mRankingController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_rankingSettingIsOn_true() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_RANKING_SCORE);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mRankingController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_chipSettingIsOff_false() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_IMPORTANCE);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mChipController.isChecked()).isFalse();

        capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_RANKING_SCORE);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mChipController.isChecked()).isFalse();

        capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_CONTEXTUAL_ACTIONS);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mChipController.isChecked()).isFalse();

        capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_TEXT_REPLIES);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mChipController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_chipSettingIsOn_true() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add(Adjustment.KEY_TEXT_REPLIES);
        capabilities.add(Adjustment.KEY_CONTEXTUAL_ACTIONS);
        when(mBackend.getAssistantAdjustments(anyString())).thenReturn(capabilities);
        assertThat(mChipController.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChange_prioritizerOn() {
        mPrioritizerController.onPreferenceChange(mPrioritizerPreference, true);
        verify(mBackend).allowAssistantAdjustment(Adjustment.KEY_IMPORTANCE, true);
    }

    @Test
    public void onPreferenceChange_prioritizerOff() {
        mPrioritizerController.onPreferenceChange(mPrioritizerPreference, false);
        verify(mBackend).allowAssistantAdjustment(Adjustment.KEY_IMPORTANCE, false);
    }

    @Test
    public void onPreferenceChange_rankingOn() {
        mRankingController.onPreferenceChange(mRankingPreference, true);
        verify(mBackend).allowAssistantAdjustment(Adjustment.KEY_RANKING_SCORE, true);
    }

    @Test
    public void onPreferenceChange_rankingOff() {
        mRankingController.onPreferenceChange(mRankingPreference, false);
        verify(mBackend).allowAssistantAdjustment(Adjustment.KEY_RANKING_SCORE, false);
    }

    @Test
    public void onPreferenceChange_chipsOn() {
        mChipController.onPreferenceChange(mChipPreference, true);
        verify(mBackend).allowAssistantAdjustment(Adjustment.KEY_CONTEXTUAL_ACTIONS, true);
        verify(mBackend).allowAssistantAdjustment(Adjustment.KEY_TEXT_REPLIES, true);
    }

    @Test
    public void onPreferenceChange_chipsOff() {
        mChipController.onPreferenceChange(mChipPreference, false);
        verify(mBackend).allowAssistantAdjustment(Adjustment.KEY_CONTEXTUAL_ACTIONS, false);
        verify(mBackend).allowAssistantAdjustment(Adjustment.KEY_TEXT_REPLIES, false);
    }
}

