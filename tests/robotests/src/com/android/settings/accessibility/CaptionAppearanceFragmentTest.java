/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

/** Tests for {@link CaptionAppearanceFragment}. */
@RunWith(RobolectricTestRunner.class)
public class CaptionAppearanceFragmentTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private SettingsActivity mActivity;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private PreferenceCategory mCustomPref;
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    private TestCaptionAppearanceFragment mFragment;

    @Before
    public void setUp() {
        mFragment = spy(new TestCaptionAppearanceFragment());
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mCustomPref).when(mFragment).findPreference(mFragment.PREF_CUSTOM);
        when(mPreferenceManager.getPreferenceScreen()).thenReturn(mScreen);
        ReflectionHelpers.setField(mFragment, "mPreferenceManager", mPreferenceManager);
    }

    @Test
    public void onCreatePreferences_shouldPreferenceIsInvisible() {
        mFragment.onAttach(mContext);

        mFragment.onCreatePreferences(Bundle.EMPTY, /* rootKey */ null);

        verify(mCustomPref).setVisible(false);
    }

    @Test
    public void onCreatePreferences_customValue_shouldPreferenceIsVisible() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, CaptionStyle.PRESET_CUSTOM);
        mFragment.onAttach(mContext);

        mFragment.onCreatePreferences(Bundle.EMPTY, /* rootKey */ null);

        verify(mCustomPref).setVisible(true);
    }

    @Test
    public void onStart_registerSpecificContentObserverForSpecificKeys() {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        mFragment.onAttach(mContext);
        mFragment.onCreatePreferences(Bundle.EMPTY, /* rootKey */ null);

        mFragment.onStart();

        for (String key : mFragment.CAPTIONING_FEATURE_KEYS) {
            verify(mContentResolver).registerContentObserver(Settings.Secure.getUriFor(key),
                    /* notifyForDescendants= */ false, mFragment.mSettingsContentObserver);
        }
    }

    @Test
    public void onStop_unregisterContentObserver() {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        mFragment.onAttach(mContext);
        mFragment.onCreatePreferences(Bundle.EMPTY, /* rootKey */ null);
        mFragment.onStart();

        mFragment.onStop();

        verify(mContentResolver).unregisterContentObserver(mFragment.mSettingsContentObserver);
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_CAPTION_APPEARANCE);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("CaptionAppearanceFragment");
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = CaptionAppearanceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext,
                R.xml.captioning_appearance);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    private static class TestCaptionAppearanceFragment extends CaptionAppearanceFragment {

        @Override
        public int getPreferenceScreenResId() {
            return R.xml.placeholder_prefs;
        }
    }
}
