/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/** Tests for {@link AccessibilityButtonFragment}. */
@Config(shadows = ShadowFragment.class)
@RunWith(RobolectricTestRunner.class)
public class AccessibilityButtonFragmentTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private Resources mResources = spy(mContext.getResources());
    private AccessibilityButtonFragment mFragment;

    @Before
    public void setUp() {
        mFragment = spy(new TestAccessibilityButtonFragment(mContext));
        when(mContext.getResources()).thenReturn(mResources);
        when(mFragment.getResources()).thenReturn(mResources);
        when(mFragment.getActivity()).thenReturn(Robolectric.setupActivity(FragmentActivity.class));
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_BUTTON_SETTINGS);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("AccessibilityButtonFragment");
    }

    @Test
    public void onCreate_navigationGestureEnabled_setCorrectTitle() {
        when(mResources.getInteger(com.android.internal.R.integer.config_navBarInteractionMode))
                .thenReturn(NAV_BAR_MODE_GESTURAL);

        mFragment.onAttach(mContext);
        mFragment.onCreate(Bundle.EMPTY);

        assertThat(mFragment.getActivity().getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_button_gesture_title));
    }

    @Test
    public void onCreate_navigationGestureDisabled_setCorrectTitle() {
        when(mResources.getInteger(com.android.internal.R.integer.config_navBarInteractionMode))
                .thenReturn(NAV_BAR_MODE_2BUTTON);

        mFragment.onAttach(mContext);
        mFragment.onCreate(Bundle.EMPTY);

        assertThat(mFragment.getActivity().getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_button_title));
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = AccessibilityButtonFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_button_settings);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    private static class TestAccessibilityButtonFragment extends AccessibilityButtonFragment {

        private final Context mContext;
        private final PreferenceManager mPreferenceManager;

        TestAccessibilityButtonFragment(Context context) {
            super();
            mContext = context;
            mPreferenceManager = new PreferenceManager(context);
            mPreferenceManager.setPreferences(mPreferenceManager.createPreferenceScreen(context));
        }

        @Override
        public int getPreferenceScreenResId() {
            return R.xml.placeholder_prefs;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceManager.getPreferenceScreen();
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPreferenceManager;
        }

        @Override
        public Context getContext() {
            return mContext;
        }
    }
}
