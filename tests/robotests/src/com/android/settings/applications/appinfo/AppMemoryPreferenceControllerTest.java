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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.SettingsActivity;
import com.android.settings.applications.ProcStatsData;
import com.android.settings.applications.ProcessStatsDetail;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class AppMemoryPreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

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
        mContext = spy(ApplicationProvider.getApplicationContext());
        UserManager userManager = mock(UserManager.class);
        when(userManager.isAdminUser()).thenReturn(true);
        doReturn(userManager).when(mContext).getSystemService(Context.USER_SERVICE);

        mController =
                spy(new AppMemoryPreferenceController(mContext, mFragment, null /* lifecycle */));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
        when(mFragment.getActivity()).thenReturn(mActivity);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_developmentSettingsEnabled_shouldReturnAvailable() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_devSettingsEnabled_butNotVisible_shouldReturnUnsupported() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_devSettingsDisabled_butNotVisible_shouldReturnUnsupported() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_developmentSettingsDisabled_shouldReturnDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartProcessStatsDetail() {
        final ProcStatsData data = mock(ProcStatsData.class);
        when(data.getMemInfo()).thenReturn(mock(ProcStatsData.MemInfo.class));
        ReflectionHelpers.setField(mController, "mStatsManager", data);

        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(mActivity).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ProcessStatsDetail.class.getName());
    }
}
