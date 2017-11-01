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


import android.app.Activity;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingsPreferenceFragmentTest {

    private static final int ITEM_COUNT = 5;

    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private Activity mActivity;
    @Mock
    private View mListContainer;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    private Context mContext;
    private TestFragment mFragment;
    private View mEmptyView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFragment = spy(new TestFragment());
        doReturn(mActivity).when(mFragment).getActivity();

        mEmptyView = new View(mContext);
        ReflectionHelpers.setField(mFragment, "mEmptyView", mEmptyView);

        doReturn(ITEM_COUNT).when(mPreferenceScreen).getPreferenceCount();
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

    @Test
    public void testUpdateEmptyView_containerInvisible_emptyViewVisible() {
        doReturn(View.INVISIBLE).when(mListContainer).getVisibility();
        doReturn(mListContainer).when(mActivity).findViewById(android.R.id.list_container);
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();

        mFragment.updateEmptyView();

        assertThat(mEmptyView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testUpdateEmptyView_containerNull_emptyViewGone() {
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();

        mFragment.updateEmptyView();

        assertThat(mEmptyView.getVisibility()).isEqualTo(View.GONE);
    }

    public static class TestFragment extends SettingsPreferenceFragment {

        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }


}
