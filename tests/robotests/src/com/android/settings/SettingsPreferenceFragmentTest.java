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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.widget.WorkOnlyCategory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class SettingsPreferenceFragmentTest {

    private static final int ITEM_COUNT = 5;

    @Mock
    private FragmentActivity mActivity;
    @Mock
    private View mListContainer;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    private Context mContext;
    private TestFragment mFragment;
    private TestFragment2 mFragment2;
    private View mEmptyView;
    private int mInitDeviceProvisionedValue;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mContext = RuntimeEnvironment.application;
        mFragment = spy(new TestFragment());
        mFragment2 = spy(new TestFragment2());
        doReturn(mActivity).when(mFragment).getActivity();
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment2.getContext()).thenReturn(mContext);

        mEmptyView = new View(mContext);
        ReflectionHelpers.setField(mFragment, "mEmptyView", mEmptyView);

        doReturn(ITEM_COUNT).when(mPreferenceScreen).getPreferenceCount();

        mInitDeviceProvisionedValue = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, mInitDeviceProvisionedValue);
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

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onCreate_hasExtraFragmentKey_shouldExpandPreferences() {
        doReturn(mContext.getTheme()).when(mActivity).getTheme();
        doReturn(mContext.getResources()).when(mFragment).getResources();
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        final Bundle bundle = new Bundle();
        bundle.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, "test_key");
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.onCreate(null /* icicle */);

        verify(mPreferenceScreen).setInitialExpandedChildrenCount(Integer.MAX_VALUE);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onCreate_noPreferenceScreen_shouldNotCrash() {
        doReturn(mContext.getTheme()).when(mActivity).getTheme();
        doReturn(mContext.getResources()).when(mFragment).getResources();
        doReturn(null).when(mFragment).getPreferenceScreen();
        final Bundle bundle = new Bundle();
        bundle.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, "test_key");
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.onCreate(null /* icicle */);
        // no crash
    }

    @Test
    public void checkAvailablePrefs_selfAvialbalePreferenceNotAvailable_shouldHidePreference() {
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        final WorkOnlyCategory workOnlyCategory = mock(WorkOnlyCategory.class);
        when(mPreferenceScreen.getPreferenceCount()).thenReturn(1);
        when(mPreferenceScreen.getPreference(0)).thenReturn(workOnlyCategory);
        when(workOnlyCategory.isAvailable(any(Context.class))).thenReturn(false);

        mFragment.checkAvailablePrefs(mPreferenceScreen);

        verify(mPreferenceScreen, never()).removePreference(workOnlyCategory);
        verify(workOnlyCategory).setVisible(false);
    }

    @Test
    public void showPinnedHeader_shouldBeVisible() {
        mFragment.mPinnedHeaderFrameLayout = new FrameLayout(mContext);

        mFragment.showPinnedHeader(true);

        assertThat(mFragment.mPinnedHeaderFrameLayout.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void hidePinnedHeader_shouldBeInvisible() {
        mFragment.mPinnedHeaderFrameLayout = new FrameLayout(mContext);

        mFragment.showPinnedHeader(false);

        assertThat(mFragment.mPinnedHeaderFrameLayout.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onAttach_shouldNotSkipForSUWAndDeviceIsProvisioned_notCallFinish() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        mFragment.onAttach(mContext);

        verify(mFragment, never()).finish();
    }

    @Test
    public void onAttach_shouldNotSkipForSUWAndDeviceIsNotProvisioned_notCallFinish() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);

        mFragment.onAttach(mContext);

        verify(mFragment, never()).finish();
    }

    @Test
    public void onAttach_shouldSkipForSUWAndDeviceIsDeviceProvisioned_notCallFinish() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        mFragment2.onAttach(mContext);

        verify(mFragment2, never()).finish();
    }

    @Test
    public void onAttach_shouldSkipForSUWAndDeviceProvisioned_notCallFinish() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);

        mFragment2.onAttach(mContext);

        verify(mFragment2, times(1)).finish();
    }

    public static class TestFragment extends SettingsPreferenceFragment {

        @Override
        protected boolean shouldSkipForInitialSUW() {
            return false;
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }

    public static class TestFragment2 extends SettingsPreferenceFragment {

        @Override
        protected boolean shouldSkipForInitialSUW() {
            return true;
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }
}
