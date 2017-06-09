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
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ProgressiveDisclosureTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceFragment mPreferenceFragment;
    @Mock
    private ExpandPreference mExpandButton;
    private PreferenceScreen mScreen;
    private Context mAppContext;
    private Preference mPreference;
    private ProgressiveDisclosureMixin mMixin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mScreen = mPreferenceFragment.getPreferenceScreen();
        mAppContext = ShadowApplication.getInstance().getApplicationContext();
        mMixin = new ProgressiveDisclosureMixin(mAppContext,
                mPreferenceFragment, false /* keepExpanded */);
        ReflectionHelpers.setField(mMixin, "mExpandButton", mExpandButton);
        mPreference = new Preference(mAppContext);
        mPreference.setKey("test");
    }

    @Test
    public void shouldNotCollapse_lessPreferenceThanLimit() {
        when(mScreen.getPreferenceCount()).thenReturn(5);

        mMixin.setTileLimit(10);

        assertThat(mMixin.shouldCollapse(mScreen)).isFalse();
    }

    @Test
    public void shouldNotCollapse_preferenceCountSameAsThreshold() {
        when(mScreen.getPreferenceCount()).thenReturn(5);

        mMixin.setTileLimit(5);

        assertThat(mMixin.shouldCollapse(mScreen)).isFalse();
    }

    @Test
    public void shouldNotCollapse_whenStartAsExpanded() {
        when(mScreen.getPreferenceCount()).thenReturn(5);

        mMixin = new ProgressiveDisclosureMixin(mAppContext,
                mPreferenceFragment, true /* keepExpanded */);
        mMixin.setTileLimit(10);

        assertThat(mMixin.shouldCollapse(mScreen)).isFalse();
    }

    @Test
    public void shouldCollapse_morePreferenceThanLimit() {
        when(mScreen.getPreferenceCount()).thenReturn(5);
        mMixin.setTileLimit(3);

        assertThat(mMixin.shouldCollapse(mScreen)).isTrue();
    }

    @Test
    public void findPreference_prefInCollapsedList_shouldFindIt() {
        when(mScreen.findPreference(nullable(String.class))).thenReturn(null);
        mMixin.addToCollapsedList(mPreference);

        Preference pref = mMixin.findPreference(mScreen, mPreference.getKey());

        assertThat(pref).isNotNull();
        assertThat(pref).isSameAs(mPreference);
    }

    @Test
    public void findPreference_prefOnScreen_shouldFindIt() {
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);

        Preference pref = mMixin.findPreference(mScreen, mPreference.getKey());

        assertThat(pref).isNotNull();
        assertThat(pref).isSameAs(mPreference);
    }

    @Test
    public void findPreference_prefNotInCollapsedListOrScreen_shouldNotFindIt() {
        when(mScreen.findPreference(nullable(String.class))).thenReturn(null);
        Preference pref = mMixin.findPreference(mScreen, mPreference.getKey());

        assertThat(pref).isNull();
    }

    @Test
    public void findPreference_prefRemovedFromCollapsedList_shouldNotFindIt() {
        when(mScreen.findPreference(nullable(String.class))).thenReturn(null);
        mMixin.addToCollapsedList(mPreference);
        mMixin.removePreference(mPreferenceFragment.getPreferenceScreen(), mPreference.getKey());

        Preference pref = mMixin.findPreference(mScreen, mPreference.getKey());

        assertThat(pref).isNull();
    }

    @Test
    public void findPreference_nestedPrefInCollapsedList_shouldFindIt() {
        when(mScreen.findPreference(nullable(String.class))).thenReturn(null);
        final PreferenceScreen prefGroup = spy(new PreferenceScreen(mAppContext, null));
        when(prefGroup.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        final Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn("TestKey");
        prefGroup.addPreference(preference);
        mMixin.addToCollapsedList(prefGroup);

        Preference pref = mMixin.findPreference(mScreen, "TestKey");

        assertThat(pref).isNotNull();
        assertThat(pref).isSameAs(preference);
    }

    @Test
    public void removePreference_shouldRemoveOnScreenPreference() {
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);

        mMixin.removePreference(mScreen, mPreference.getKey());

        verify(mScreen).removePreference(mPreference);
    }

    @Test
    public void removeLastPreference_shouldRemoveExpandButtonToo() {
        when(mScreen.findPreference(nullable(String.class))).thenReturn(null);
        mMixin.addToCollapsedList(mPreference);
        // Collapsed
        assertThat(mMixin.isCollapsed()).isTrue();

        mMixin.removePreference(mScreen, mPreference.getKey());

        // Removing expand button
        verify(mScreen).removePreference(any(Preference.class));
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
        verify(mExpandButton, never()).setSummary(nullable(String.class));
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
        verify(mExpandButton, atLeastOnce()).setSummary(nullable(String.class));
        verify(screen).addPreference(any(ExpandPreference.class));
        verify(screen, times(3)).removePreference(any(Preference.class));
    }

    @Test
    public void addToCollapsedList_shouldAddInOrder() {
        final Preference pref1 = new Preference(mAppContext);
        final Preference pref2 = new Preference(mAppContext);
        pref1.setOrder(10);
        pref2.setOrder(20);

        // Pref1 has lower order than pref2, but add pref2 first. The collapsed list should maintain
        // items in increasing order.
        mMixin.addToCollapsedList(pref2);
        mMixin.addToCollapsedList(pref1);

        List<Preference> collapsedList = mMixin.getCollapsedPrefs();
        assertThat(collapsedList.get(0)).isSameAs(pref1);
        assertThat(collapsedList.get(1)).isSameAs(pref2);
    }

    @Test
    public void addPreferenceWhenCollapsed_noPrefOnScreen_shouldAddToList() {
        // Add something to collapsed list so we are in collapsed state.
        mMixin.addToCollapsedList(new Preference(mAppContext));
        assertThat(mMixin.getCollapsedPrefs().size()).isEqualTo(1);

        // Just 1 preference on screen: the more button
        when(mScreen.getPreferenceCount()).thenReturn(1);
        final Preference toBeAdded = new Preference(mAppContext);
        toBeAdded.setOrder(100);
        mMixin.addPreference(mScreen, toBeAdded);

        // Should have 2 prefs in collapsed list now
        assertThat(mMixin.getCollapsedPrefs().size()).isEqualTo(2);
        assertThat(mMixin.getCollapsedPrefs().get(0)).isSameAs(toBeAdded);
    }

    @Test
    public void addPreferenceWhenCollapsed_prefOrderLessThanLastOnScreen_shouldAddToScreen() {
        final Preference lastPref = new Preference(mAppContext);
        lastPref.setOrder(100);
        // Add something to collapsed list so we are in collapsed state.
        mMixin.addToCollapsedList(new Preference(mAppContext));
        verify(mExpandButton).setSummary(nullable(String.class));
        assertThat(mMixin.getCollapsedPrefs().size()).isEqualTo(1);

        // 3 prefs on screen, 2 are real and the last one is more button.
        when(mScreen.getPreferenceCount()).thenReturn(3);
        when(mScreen.getPreference(1)).thenReturn(lastPref);

        final Preference toBeAdded = new Preference(mAppContext);
        toBeAdded.setOrder(50);
        mMixin.addPreference(mScreen, toBeAdded);

        verify(mScreen).removePreference(lastPref);
        verify(mScreen).addPreference(toBeAdded);
        assertThat(mMixin.getCollapsedPrefs().get(0)).isSameAs(lastPref);
    }

    @Test
    public void addPreferenceWhenCollapsed_prefOrderMoreThanLastOnScreen_shouldAddToList() {
        final Preference lastPref = new Preference(mAppContext);
        lastPref.setOrder(100);
        // Add something to collapsed list so we are in collapsed state.
        mMixin.addToCollapsedList(new Preference(mAppContext));
        verify(mExpandButton).setSummary(nullable(String.class));
        assertThat(mMixin.getCollapsedPrefs().size()).isEqualTo(1);

        // 3 prefs on screen, 2 are real and the last one is more button.
        when(mScreen.getPreferenceCount()).thenReturn(3);
        when(mScreen.getPreference(1)).thenReturn(lastPref);

        final Preference toBeAdded = new Preference(mAppContext);
        toBeAdded.setOrder(200);
        mMixin.addPreference(mScreen, toBeAdded);

        verify(mScreen, never()).removePreference(any(Preference.class));
        verify(mScreen, never()).addPreference(any(Preference.class));
        verify(mExpandButton, times(2)).setSummary(nullable(String.class));
        assertThat(mMixin.getCollapsedPrefs().get(0)).isSameAs(toBeAdded);
    }

    @Test
    public void updateExpandSummary_noPref_noSummary() {
        mMixin.updateExpandButtonSummary();

        verify(mExpandButton).setSummary(null);
    }

    @Test
    public void updateExpandSummary_doNotIncludeEmptyPrefTitle() {
        final Preference pref1 = new Preference(mAppContext);
        pref1.setTitle("1");
        final Preference pref2 = new Preference(mAppContext);
        pref2.setTitle(null);
        final Preference pref3 = new Preference(mAppContext);
        pref3.setTitle("3");
        final Preference pref4 = new Preference(mAppContext);
        pref4.setTitle("");

        mMixin.addToCollapsedList(pref1);
        mMixin.addToCollapsedList(pref2);
        mMixin.addToCollapsedList(pref3);
        mMixin.addToCollapsedList(pref4);

        verify(mExpandButton).setSummary("1, 3");
    }

    @Test
    public void updateExapndSummary_singlePref_expandSummarySameAsPrefTitle() {
        final String TEST = "test";
        final Preference pref = new Preference(mAppContext);
        pref.setTitle(TEST);

        mMixin.addToCollapsedList(pref);
        verify(mExpandButton).setSummary(TEST);
    }

    @Test
    public void updateExapndSummary_multiPrefs_useCombinedPrefTitleAsSummary() {
        final String TEST1 = "test1";
        final String TEST2 = "test2";
        final Preference pref1 = new Preference(mAppContext);
        pref1.setTitle(TEST1);
        final Preference pref2 = new Preference(mAppContext);
        pref2.setTitle(TEST2);

        mMixin.addToCollapsedList(pref1);
        mMixin.addToCollapsedList(pref2);

        verify(mExpandButton)
                .setSummary(mAppContext.getString(R.string.join_many_items_middle, TEST1, TEST2));
    }
}
