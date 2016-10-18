/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ProgressiveDisclosureTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private FakeFeatureFactory mFakeFeatureFactory;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceFragment mPreferenceFragment;
    private Context mAppContext;
    private Preference mPreference;

    private ProgressiveDisclosureMixin mMixin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mAppContext = ShadowApplication.getInstance().getApplicationContext();
        mFakeFeatureFactory = (FakeFeatureFactory) FeatureFactory.getFactory(mContext);
        mMixin = new ProgressiveDisclosureMixin(mAppContext, mPreferenceFragment);
        mPreference = new Preference(mAppContext);
        mPreference.setKey("test");
        when(mFakeFeatureFactory.dashboardFeatureProvider.isEnabled()).thenReturn(true);
    }

    @Test
    public void shouldNotCollapse_lessPreferenceThanLimit() {
        when(mPreferenceFragment.getPreferenceScreen().getPreferenceCount()).thenReturn(5);

        mMixin.setTileLimit(10);

        assertThat(mMixin.shouldCollapse(mPreferenceFragment.getPreferenceScreen())).isFalse();
    }

    @Test
    public void shouldCollapse_morePreferenceThanLimit() {
        when(mFakeFeatureFactory.dashboardFeatureProvider.isEnabled()).thenReturn(true);
        when(mPreferenceFragment.getPreferenceScreen().getPreferenceCount()).thenReturn(5);

        assertThat(mMixin.shouldCollapse(mPreferenceFragment.getPreferenceScreen())).isTrue();
    }

    @Test
    public void findPreference_prefInCollapsedList_shouldFindIt() {
        mMixin.addToCollapsedList(mPreference);

        Preference pref = mMixin.findPreference(mPreference.getKey());

        assertThat(pref).isNotNull();
        assertThat(pref).isSameAs(mPreference);
    }

    @Test
    public void findPreference_prefNotInCollapsedList_shouldNotFindIt() {
        Preference pref = mMixin.findPreference(mPreference.getKey());

        assertThat(pref).isNull();
    }

    @Test
    public void findPreference_prefRemovedFromCollapsedList_shouldNotFindIt() {
        mMixin.addToCollapsedList(mPreference);
        mMixin.removePreference(mPreferenceFragment.getPreferenceScreen(), mPreference.getKey());
        Preference pref = mMixin.findPreference(mPreference.getKey());

        assertThat(pref).isNull();
    }

    @Test
    public void removeLastPreference_shouldRemoveExpandButtonToo() {
        mMixin.addToCollapsedList(mPreference);
        // Collapsed
        assertThat(mMixin.isCollapsed()).isTrue();

        mMixin.removePreference(mPreferenceFragment.getPreferenceScreen(), mPreference.getKey());

        // Removing expand button
        verify(mPreferenceFragment.getPreferenceScreen()).removePreference(any(Preference.class));
        // No longer collapsed
        assertThat(mMixin.isCollapsed()).isFalse();
    }

    @Test
    public void collapse_shouldDoNothingIfNotCollapsible() {
        final PreferenceScreen screen = mPreferenceFragment.getPreferenceScreen();
        when(screen.getPreferenceCount()).thenReturn(5);
        mMixin.setTileLimit(15);

        mMixin.collapse(screen);
        assertThat(mMixin.isCollapsed()).isFalse();
        verify(screen, never()).addPreference(any(Preference.class));
        verify(screen, never()).removePreference(any(Preference.class));
    }

    @Test
    public void collapse_shouldRemovePrefAndAddExpandButton() {
        final PreferenceScreen screen = mPreferenceFragment.getPreferenceScreen();
        when(screen.getPreferenceCount()).thenReturn(5);
        when(screen.getPreference(anyInt())).thenReturn(mPreference);
        mMixin.setTileLimit(2);

        mMixin.collapse(screen);

        assertThat(mMixin.isCollapsed()).isTrue();
        verify(screen).addPreference(any(ExpandPreference.class));
        verify(screen, times(3)).removePreference(any(Preference.class));
    }

}
