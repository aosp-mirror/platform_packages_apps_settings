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

import static com.android.settings.applications.appinfo.AppInfoDashboardFragment.SUB_INFO_FRAGMENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.notification.app.AppNotificationSettings;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class AppInfoPreferenceControllerBaseTest {

    @Mock
    private SettingsActivity mActivity;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;

    private TestPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new TestPreferenceController(mFragment);
        final String key = mController.getPreferenceKey();
        when(mScreen.findPreference(key)).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(key);
        when(mFragment.getContext()).thenReturn(mActivity);
        when(mFragment.getActivity()).thenReturn(mActivity);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void refreshUi_shouldUpdatePreference() {
        mController.displayPreference(mScreen);
        mController.refreshUi();

        assertThat(mController.preferenceUpdated).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartDetailFragmentClass() {
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = new ApplicationInfo();
        when(mFragment.getAppEntry()).thenReturn(appEntry);

        mController.handlePreferenceTreeClick(mPreference);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(mFragment).startActivityForResult(intentCaptor.capture(), eq(SUB_INFO_FRAGMENT));
        assertThat(intentCaptor.getValue().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(mController.getDetailFragmentClass().getName());
    }

    private class TestPreferenceController extends AppInfoPreferenceControllerBase {

        private boolean preferenceUpdated;

        private TestPreferenceController(AppInfoDashboardFragment parent) {
            super(RuntimeEnvironment.application, "TestKey");
            setParentFragment(parent);
        }

        @Override
        public void updateState(Preference preference) {
            preferenceUpdated = true;
        }

        @Override
        public Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
            return AppNotificationSettings.class;
        }

        @Override
        protected Bundle getArguments() {
            Bundle bundle = new Bundle();
            bundle.putString("test", "test");
            return bundle;
        }
    }
}
