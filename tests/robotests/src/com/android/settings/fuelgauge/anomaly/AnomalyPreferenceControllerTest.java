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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.ButtonActionDialogFragmentTest;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDynamicIndexableContentMonitor;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.FragmentTestUtil;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AnomalyPreferenceControllerTest {
    @Anomaly.AnomalyType
    private static final int ANOMALY_TYPE = Anomaly.AnomalyType.WAKE_LOCK;
    private static final String PACKAGE_NAME = "com.android.app";
    private static final int UID = 111;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceFragment mFragment;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    private AnomalyPreferenceController mAnomalyPreferenceController;
    private Preference mPreference;
    private Context mContext;
    private List<Anomaly> mAnomalyList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mPreference.setKey(AnomalyPreferenceController.ANOMALY_KEY);
        when(mFragment.getPreferenceManager().findPreference(any())).thenReturn(mPreference);
        when(mFragment.getFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);

        mAnomalyList = new ArrayList<>();
        Anomaly anomaly = new Anomaly.Builder()
                .setType(ANOMALY_TYPE)
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .build();
        mAnomalyList.add(anomaly);

        mAnomalyPreferenceController = new AnomalyPreferenceController(mFragment);
    }

    @Test
    public void testUpdateAnomalyPreference_hasCorrectData() {
        mAnomalyPreferenceController.updateAnomalyPreference(mAnomalyList);

        //add more test when this method is complete
        assertThat(mAnomalyPreferenceController.mAnomalies).isEqualTo(mAnomalyList);
    }

    @Test
    public void testOnPreferenceTreeClick_oneAnomaly_showDialog() {
        mAnomalyPreferenceController.mAnomalies = mAnomalyList;

        mAnomalyPreferenceController.onPreferenceTreeClick(mPreference);

        verify(mFragmentManager).beginTransaction();
        verify(mFragmentTransaction).add(any(), anyString());
        verify(mFragmentTransaction).commit();
    }

}
