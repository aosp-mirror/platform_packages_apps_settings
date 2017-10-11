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

package com.android.settings;


import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingsPreferenceFragmentTest {

    @Mock
    private PreferenceManager mPreferenceManager;
    private Context mContext;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFragment = new TestFragment();
    }

    @Test
    public void removePreference_nested_shouldRemove() {
        final String key = "test_key";
        final PreferenceScreen mScreen = spy(new PreferenceScreen(mContext, null));
        when(mScreen.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));

        final PreferenceCategory nestedCategory = new ProgressCategory(mContext);
        final Preference preference = new Preference(mContext);
        preference.setKey(key);
        preference.setPersistent(false);

        mScreen.addPreference(nestedCategory);
        nestedCategory.addPreference(preference);

        assertThat(mFragment.removePreference(mScreen, key)).isTrue();
        assertThat(nestedCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void removePreference_flat_shouldRemove() {
        final String key = "test_key";
        final PreferenceScreen mScreen = spy(new PreferenceScreen(mContext, null));
        when(mScreen.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));

        final Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(key);

        mScreen.addPreference(preference);

        assertThat(mFragment.removePreference(mScreen, key)).isTrue();
        assertThat(mScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void removePreference_doNotExist_shouldNotRemove() {
        final String key = "test_key";
        final PreferenceScreen mScreen = spy(new PreferenceScreen(mContext, null));
        when(mScreen.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));

        final Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(key);

        mScreen.addPreference(preference);

        assertThat(mFragment.removePreference(mScreen, "not" + key)).isFalse();
        assertThat(mScreen.getPreferenceCount()).isEqualTo(1);
    }

    public static final class TestFragment extends SettingsPreferenceFragment {

        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }


}
