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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.util.IconDrawableFactory;

import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.anomaly.Anomaly;

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
    private static final String NAME_APP_3 = "app3";
    private static final String PACKAGE_NAME_1 = "com.android.app1";
    private static final String PACKAGE_NAME_2 = "com.android.app2";
    private static final String PACKAGE_NAME_3 = "com.android.app3";
    private static final int USER_ID = 1;

    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private Drawable mDrawable1;
    @Mock
    private Drawable mDrawable2;
    @Mock
    private Drawable mDrawable3;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private IconDrawableFactory mIconDrawableFactory;
    @Mock
    private ApplicationInfo mApplicationInfo;
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
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .setPackageName(PACKAGE_NAME_2)
                .setDisplayName(NAME_APP_2)
                .build();
        mAnomalyList.add(anomaly2);
        Anomaly anomaly3 = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.BLUETOOTH_SCAN)
                .setPackageName(PACKAGE_NAME_3)
                .setDisplayName(NAME_APP_3)
                .build();
        mAnomalyList.add(anomaly3);

        mFragment = spy(new PowerUsageAnomalyDetails());
        mFragment.mAbnormalListGroup = mAbnormalListGroup;
        mFragment.mAnomalies = mAnomalyList;
        mFragment.mBatteryUtils = new BatteryUtils(mContext);
        mFragment.mPackageManager = mPackageManager;
        mFragment.mIconDrawableFactory = mIconDrawableFactory;
        doReturn(mPreferenceManager).when(mFragment).getPreferenceManager();
        doReturn(mContext).when(mPreferenceManager).getContext();
    }

    @Test
    public void testRefreshUi_displayCorrectTitleAndSummary() {
        final List<Preference> testPreferences = new ArrayList<>();
        final ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(
                Preference.class);
        Answer<Void> prefCallable = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                testPreferences.add(preferenceCaptor.getValue());
                return null;
            }
        };
        doAnswer(prefCallable).when(mAbnormalListGroup).addPreference(preferenceCaptor.capture());

        mFragment.refreshUi();

        final Preference wakelockPreference = testPreferences.get(0);
        assertThat(wakelockPreference.getTitle()).isEqualTo(NAME_APP_1);
        assertThat(wakelockPreference.getSummary()).isEqualTo("Keeping device awake");
        final Preference wakeupPreference = testPreferences.get(1);
        assertThat(wakeupPreference.getTitle()).isEqualTo(NAME_APP_2);
        assertThat(wakeupPreference.getSummary()).isEqualTo("Waking up device in background");
        final Preference bluetoothPreference = testPreferences.get(2);
        assertThat(bluetoothPreference.getTitle()).isEqualTo(NAME_APP_3);
        assertThat(bluetoothPreference.getSummary()).isEqualTo("Requesting location frequently");
    }

    @Test
    public void testRefreshUi_iconCorrect() {
        doReturn(mDrawable1).when(mFragment).getBadgedIcon(eq(PACKAGE_NAME_1), anyInt());
        doReturn(mDrawable2).when(mFragment).getBadgedIcon(eq(PACKAGE_NAME_2), anyInt());
        doReturn(mDrawable3).when(mFragment).getBadgedIcon(eq(PACKAGE_NAME_3), anyInt());

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

        assertThat(testIcons).containsExactly(mDrawable1, mDrawable2, mDrawable3);
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

    @Test
    public void testGetBadgedIcon_usePackageNameAndUserId() throws
            PackageManager.NameNotFoundException {
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(PACKAGE_NAME_1,
                PackageManager.GET_META_DATA);

        mFragment.getBadgedIcon(PACKAGE_NAME_1, USER_ID);

        // Verify that it uses the correct user id
        verify(mIconDrawableFactory).getBadgedIcon(mApplicationInfo, USER_ID);
    }
}
