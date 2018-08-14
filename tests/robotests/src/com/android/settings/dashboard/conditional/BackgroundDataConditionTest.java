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
 */
package com.android.settings.dashboard.conditional;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.util.FeatureFlagUtils;

import com.android.settings.Settings;
import com.android.settings.core.FeatureFlags;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class BackgroundDataConditionTest {
    @Mock
    private ConditionManager mConditionManager;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mConditionManager.getContext()).thenReturn(mContext);
    }

    @Test
    public void onPrimaryClick_v2enabled_shouldReturn2SummaryActivity() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.DATA_USAGE_SETTINGS_V2, true);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        BackgroundDataCondition backgroundDataCondition
                = new BackgroundDataCondition(mConditionManager);
        backgroundDataCondition.onPrimaryClick();
        verify(mContext).startActivity(argumentCaptor.capture());
        Intent intent = argumentCaptor.getValue();

        assertThat(intent.getComponent().getClassName()).isEqualTo(
                Settings.DataUsageSummaryActivity.class.getName());
    }

    @Test
    public void onPrimaryClick_v2disabled_shouldReturnLegacySummaryActivity() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.DATA_USAGE_SETTINGS_V2, false);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        BackgroundDataCondition backgroundDataCondition
                = new BackgroundDataCondition(mConditionManager);
        backgroundDataCondition.onPrimaryClick();
        verify(mContext).startActivity(argumentCaptor.capture());
        Intent intent = argumentCaptor.getValue();

        assertThat(intent.getComponent().getClassName()).isEqualTo(
                Settings.DataUsageSummaryLegacyActivity.class.getName());
    }
}
