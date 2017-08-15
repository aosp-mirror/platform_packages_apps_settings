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

package com.android.settings.fuelgauge.anomaly;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;

import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AnomalySummaryPreferenceControllerTest {
    @Anomaly.AnomalyType
    private static final int ANOMALY_TYPE = Anomaly.AnomalyType.WAKE_LOCK;
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String DISPLAY_NAME = "appName";
    private static final int UID = 111;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceFragment mFragment;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private SettingsActivity mSettingsActivity;
    private AnomalySummaryPreferenceController mAnomalySummaryPreferenceController;
    private Preference mPreference;
    private Context mContext;
    private List<Anomaly> mAnomalyList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mPreference.setKey(AnomalySummaryPreferenceController.ANOMALY_KEY);
        when(mFragment.getPreferenceScreen().findPreference(any())).thenReturn(mPreference);
        when(mFragment.getFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mSettingsActivity.getApplicationContext()).thenReturn(mContext);

        mAnomalyList = new ArrayList<>();

        mAnomalySummaryPreferenceController = new AnomalySummaryPreferenceController(
                mSettingsActivity, mFragment, 0 /* metricskey */);
    }

    @Test
    public void testUpdateHighUsageSummaryPreference_hasCorrectData() {
        mAnomalySummaryPreferenceController.updateAnomalySummaryPreference(mAnomalyList);

        assertThat(mAnomalySummaryPreferenceController.mAnomalies).isEqualTo(mAnomalyList);
    }

    @Test
    public void testUpdateAnomalySummaryPreference_oneAnomaly_showCorrectSummary() {
        mAnomalyList.add(createTestAnomaly());

        mAnomalySummaryPreferenceController.updateAnomalySummaryPreference(mAnomalyList);

        assertThat(mPreference.getTitle()).isEqualTo("appName draining battery");
        assertThat(mPreference.getSummary()).isEqualTo("Keeping device awake");
    }

    @Test
    public void testUpdateAnomalySummaryPreference_emptyAnomaly_preferenceInvisible() {
        mPreference.setVisible(true);
        mAnomalyList.clear();

        mAnomalySummaryPreferenceController.updateAnomalySummaryPreference(mAnomalyList);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void testUpdateAnomalySummaryPreference_multipleAnomalies_showCorrectSummary() {
        mAnomalyList.add(createTestAnomaly());
        mAnomalyList.add(createTestAnomaly());

        mAnomalySummaryPreferenceController.updateAnomalySummaryPreference(mAnomalyList);

        assertThat(mPreference.getTitle()).isEqualTo("Apps draining battery");
        assertThat(mPreference.getSummary()).isEqualTo("2 apps misbehaving");
    }

    @Test
    public void testOnPreferenceTreeClick_oneAnomaly_showDialog() {

        mAnomalyList.add(createTestAnomaly());
        mAnomalySummaryPreferenceController.mAnomalies = mAnomalyList;

        mAnomalySummaryPreferenceController.onPreferenceTreeClick(mPreference);

        verify(mFragmentManager).beginTransaction();
        verify(mFragmentTransaction).add(any(), anyString());
        verify(mFragmentTransaction).commit();
    }

    private Anomaly createTestAnomaly() {
        return new Anomaly.Builder()
                .setType(ANOMALY_TYPE)
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .setDisplayName(DISPLAY_NAME)
                .build();
    }

}
