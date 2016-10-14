/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.system;


import android.app.Activity;

import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.FakeFeatureFactory;

import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.android.settings.system.SystemDashboardFragment.SUMMARY_PROVIDER_FACTORY;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SystemDashboardFragmentTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private SummaryLoader mSummaryLoader;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mActivity);
    }

    @Test
    public void hasSummaryProvider() {
        Truth.assertThat(SUMMARY_PROVIDER_FACTORY).isNotNull();
    }

    @Test
    public void updateSummary_isListening_shouldNotifySummaryLoader() {
        final SummaryLoader.SummaryProvider summaryProvider =
                SUMMARY_PROVIDER_FACTORY.createSummaryProvider(mActivity, mSummaryLoader);
        summaryProvider.setListening(true);

        verify(mSummaryLoader).setSummary(eq(summaryProvider), anyString());
    }

    @Test
    public void updateSummary_notListening_shouldNotNotifySummaryLoader() {
        final SummaryLoader.SummaryProvider summaryProvider =
                SUMMARY_PROVIDER_FACTORY.createSummaryProvider(mActivity, mSummaryLoader);
        summaryProvider.setListening(false);

        verify(mSummaryLoader, never()).setSummary(eq(summaryProvider), anyString());
    }
}
