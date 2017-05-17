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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyPreference;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PowerUsageAnomalyDetailsTest {
    private static final String NAME_APP_1 = "app1";
    private static final String NAME_APP_2 = "app2";
    private static final String PACKAGE_NAME_1 = "com.android.app1";
    private static final String PACKAGE_NAME_2 = "com.android.app2";

    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private Drawable mDrawable1;
    @Mock
    private Drawable mDrawable2;
    private Context mContext;
    private PowerUsageAnomalyDetails mFragment;
    private PreferenceGroup mAbnormalListGroup;
    private Bundle mBundle;
    private List<Anomaly> mAnomalyList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mAbnormalListGroup = spy(new PreferenceCategory(mContext));

        mAnomalyList = new ArrayList<>();
        Anomaly anomaly1 = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKE_LOCK)
                .setPackageName(PACKAGE_NAME_1)
                .setDisplayName(NAME_APP_1)
                .build();
        mAnomalyList.add(anomaly1);
        Anomaly anomaly2 = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKE_LOCK)
                .setPackageName(PACKAGE_NAME_2)
                .setDisplayName(NAME_APP_2)
                .build();
        mAnomalyList.add(anomaly2);

        mFragment = spy(new PowerUsageAnomalyDetails());
        doReturn(null).when(mFragment).getIconFromPackageName(any());
        mFragment.mAbnormalListGroup = mAbnormalListGroup;
        mFragment.mAnomalies = mAnomalyList;
        doReturn(mPreferenceManager).when(mFragment).getPreferenceManager();
        doReturn(mContext).when(mPreferenceManager).getContext();
    }

    @Test
    public void testRefreshUi_dataCorrect() {
        final List<Anomaly> testAnomalyList = new ArrayList<>();
        final ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(
                Preference.class);
        Answer<Void> prefCallable = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                testAnomalyList.add(
                        ((AnomalyPreference) preferenceCaptor.getValue()).getAnomaly());
                return null;
            }
        };
        doAnswer(prefCallable).when(mAbnormalListGroup).addPreference(preferenceCaptor.capture());

        mFragment.refreshUi();

        assertThat(testAnomalyList).containsExactlyElementsIn(mAnomalyList);
    }

    @Test
    public void testRefreshUi_iconCorrect() {
        doReturn(mDrawable1).when(mFragment).getIconFromPackageName(PACKAGE_NAME_1);
        doReturn(mDrawable2).when(mFragment).getIconFromPackageName(PACKAGE_NAME_2);

        final List<Drawable> testIcons = new ArrayList<>();
        final ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(
                Preference.class);
        Answer<Void> prefCallable = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                testIcons.add(preferenceCaptor.getValue().getIcon());
                return null;
            }
        };
        doAnswer(prefCallable).when(mAbnormalListGroup).addPreference(preferenceCaptor.capture());

        mFragment.refreshUi();

        assertThat(testIcons).containsExactly(mDrawable1, mDrawable2);
    }

    @Test
    public void testStartBatteryAbnormalPage_dataCorrect() {
        final ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        Answer<Void> bundleCallable = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                mBundle = bundleCaptor.getValue();
                return null;
            }
        };
        doAnswer(bundleCallable).when(mSettingsActivity).startPreferencePanelAsUser(any(),
                anyString(),
                bundleCaptor.capture(), anyInt(), any(), any());

        PowerUsageAnomalyDetails.startBatteryAbnormalPage(mSettingsActivity, mFragment,
                mAnomalyList);

        assertThat(mBundle.getParcelableArrayList(
                PowerUsageAnomalyDetails.EXTRA_ANOMALY_LIST)).isEqualTo(mAnomalyList);
    }
}
