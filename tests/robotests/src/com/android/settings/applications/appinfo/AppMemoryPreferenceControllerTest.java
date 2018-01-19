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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.applications.ProcStatsData;
import com.android.settings.applications.ProcessStatsDetail;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppMemoryPreferenceControllerTest {

    @Mock
    private SettingsActivity mActivity;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;

    private Context mContext;
    private AppMemoryPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController =
                spy(new AppMemoryPreferenceController(mContext, mFragment, null /* lifecycle */));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
        when(mFragment.getActivity()).thenReturn(mActivity);
    }

    @Test
    public void getAvailabilityStatus_developmentSettingsEnabled_shouldReturnAvailable() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_devSettingsEnabled_butNotVisible_shouldReturnUnsupported() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.DISABLED_UNSUPPORTED);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_devSettingsDisabled_butNotVisible_shouldReturnUnsupported() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.DISABLED_UNSUPPORTED);
    }

    @Test
    public void getAvailabilityStatus_developmentSettingsDisabled_shouldReturnDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(mController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartProcessStatsDetail() {
        final ProcStatsData data = mock(ProcStatsData.class);
        when(data.getMemInfo()).thenReturn(mock(ProcStatsData.MemInfo.class));
        ReflectionHelpers.setField(mController, "mStatsManager", data);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mActivity).startPreferencePanel(any(), eq(ProcessStatsDetail.class.getName()), any(),
                eq(R.string.memory_usage), any(), any(), anyInt());
    }

}
