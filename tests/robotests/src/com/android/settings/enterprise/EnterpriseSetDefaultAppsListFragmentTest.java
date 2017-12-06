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
 * limitations under the License
 */

package com.android.settings.enterprise;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class EnterpriseSetDefaultAppsListFragmentTest {
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    private EnterpriseSetDefaultAppsListFragment mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ShadowApplication.getInstance().getApplicationContext();
        when(mPreferenceManager.getContext()).thenReturn(mContext);
        when(mScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        mFragment = new EnterpriseSetDefaultAppsListFragmentTestable(mPreferenceManager, mScreen);
    }

    @Test
    public void getMetricsCategory() {
        assertThat(mFragment.getMetricsCategory())
                .isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_DEFAULT_APPS);
    }

    @Test
    public void getLogTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("EnterprisePrivacySettings");
    }

    @Test
    public void getScreenResource() {
        assertThat(mFragment.getPreferenceScreenResId())
                .isEqualTo(R.xml.enterprise_set_default_apps_settings);
    }

    @Test
    public void getPreferenceControllers() {
        final List<AbstractPreferenceController> controllers = mFragment.getPreferenceControllers(mContext);
        assertThat(controllers).isNotNull();
        assertThat(controllers.size()).isEqualTo(1);
        int position = 0;
        assertThat(controllers.get(position++)).isInstanceOf(
                EnterpriseSetDefaultAppsListPreferenceController.class);
    }

    private static class EnterpriseSetDefaultAppsListFragmentTestable extends
            EnterpriseSetDefaultAppsListFragment {

        private final PreferenceManager mPreferenceManager;
        private final PreferenceScreen mPreferenceScreen;

        public EnterpriseSetDefaultAppsListFragmentTestable(PreferenceManager preferenceManager,
                PreferenceScreen screen) {
            this.mPreferenceManager = preferenceManager;
            this.mPreferenceScreen = screen;
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPreferenceManager;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceScreen;
        }
    }

}
