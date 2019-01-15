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

package com.android.settings.deviceinfo;

import static com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;
import static com.android.settings.deviceinfo.DeviceInfoSettings.NON_SIM_PREFERENCES_COUNT;
import static com.android.settings.deviceinfo.DeviceInfoSettings.SIM_PREFERENCES_COUNT;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceScreen;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.DeviceInfoUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class, ShadowConnectivityManager.class, ShadowUserManager.class})
public class DeviceInfoSettingsTest {

    @Mock
    private Activity mActivity;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SummaryLoader mSummaryLoader;
    @Mock
    private TelephonyManager mTelephonyManager;

    private Context mContext;
    private DeviceInfoSettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mContext = RuntimeEnvironment.application;
        mSettings = spy(new DeviceInfoSettings());

        doReturn(mActivity).when(mSettings).getActivity();
        doReturn(mContext).when(mSettings).getContext();
        doReturn(mContext.getTheme()).when(mActivity).getTheme();
        doReturn(mContext.getResources()).when(mSettings).getResources();
        doNothing().when(mSettings).onCreatePreferences(any(), any());

        doReturn(mScreen).when(mSettings).getPreferenceScreen();
        ShadowApplication.getInstance()
            .setSystemService(Context.TELEPHONY_SERVICE, mTelephonyManager);
    }

    @Test
    public void getPrefXml_shouldReturnDeviceInfoXml() {
        assertThat(mSettings.getPreferenceScreenResId()).isEqualTo(R.xml.device_info_settings);
    }

    @Test
    public void getSummary_shouldReturnDeviceModel() {
        final SummaryLoader.SummaryProvider mProvider =
            DeviceInfoSettings.SUMMARY_PROVIDER_FACTORY.createSummaryProvider(null, mSummaryLoader);

        mProvider.setListening(true);

        verify(mSummaryLoader).setSummary(mProvider, Build.MODEL + DeviceInfoUtils.getMsvSuffix());
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks =
            DeviceInfoSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(context);
        final int xmlId = (new DeviceInfoSettings()).getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlId);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    @Config(shadows = SettingsShadowResources.SettingsShadowTheme.class)
    public void onCreate_fromSearch_shouldNotOverrideInitialExpandedCount() {
        final Bundle args = new Bundle();
        args.putString(EXTRA_FRAGMENT_ARG_KEY, "search_key");
        mSettings.setArguments(args);

        mSettings.onCreate(null /* icicle */);

        verify(mScreen).setInitialExpandedChildrenCount(Integer.MAX_VALUE);
    }

    @Test
    @Config(shadows = SettingsShadowResources.SettingsShadowTheme.class)
    public void onCreate_singleSim_shouldAddSingleSimCount() {
        doReturn(1).when(mTelephonyManager).getPhoneCount();

        mSettings.onCreate(null /* icicle */);

        verify(mScreen).setInitialExpandedChildrenCount(
                SIM_PREFERENCES_COUNT + NON_SIM_PREFERENCES_COUNT);
    }

    @Test
    @Config(shadows = SettingsShadowResources.SettingsShadowTheme.class)
    public void onCreate_dualeSim_shouldAddDualSimCount() {
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        mSettings.onCreate(null /* icicle */);

        verify(mScreen).setInitialExpandedChildrenCount(
                2 * SIM_PREFERENCES_COUNT + NON_SIM_PREFERENCES_COUNT);
    }
}
