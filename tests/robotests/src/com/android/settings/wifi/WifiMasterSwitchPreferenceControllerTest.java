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

package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.MasterSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiMasterSwitchPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Context mMockContext;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private MasterSwitchPreference mPreference;

    private Context mContext;
    private WifiMasterSwitchPreferenceController mController;
    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mMockContext);
        mFakeFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mMockContext);
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mController = new WifiMasterSwitchPreferenceController(mContext, mMetricsFeatureProvider);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mWifiManager.getWifiState()).thenReturn(WifiManager.WIFI_STATE_DISABLED);
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onResume_shouldRegisterCallback() {
        mController.onResume();

        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void onPause_shouldUnregisterCallback() {
        mController.onResume();
        mController.onPause();

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void onStart_shouldRegisterPreferenceChangeListener() {
        mController.displayPreference(mScreen);
        mController.onStart();

        verify(mPreference).setOnPreferenceChangeListener(any(OnPreferenceChangeListener.class));
    }

    @Test
    public void onStop_shouldRegisterPreferenceChangeListener() {
        mController.displayPreference(mScreen);
        mController.onStart();

        mController.onStop();

        verify(mPreference).setOnPreferenceChangeListener(null);
    }

    @Test
    public void onSummaryChanged_shouldUpdatePreferenceSummary() {
        mController.displayPreference(mScreen);

        mController.onSummaryChanged("test summary");

        verify(mPreference).setSummary("test summary");
    }
}
