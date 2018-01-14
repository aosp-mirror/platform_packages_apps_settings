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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AccessibilitySettingsTest {

    private Context mContext;
    private AccessibilitySettings mFragment;
    private boolean mAccessibilityShortcutPreferenceRemoved;
    private boolean mColorInversionPreferenceRemoved;
    private boolean mColorCorrectionPreferenceRemoved;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFragment = new AccessibilitySettings() {
            @Override
            public Context getContext() {
                return mContext;
            }

            @Override
            protected boolean removePreference(String key) {
                if (AccessibilitySettings.ACCESSIBILITY_SHORTCUT_PREFERENCE.equals(key)) {
                    mAccessibilityShortcutPreferenceRemoved = true;
                    return true;
                }

                if (AccessibilitySettings.TOGGLE_INVERSION_PREFERENCE.equals(key)) {
                    mColorInversionPreferenceRemoved = true;
                    return true;
                }

                if (AccessibilitySettings.DISPLAY_DALTONIZER_PREFERENCE_SCREEN.equals(key)) {
                    mColorCorrectionPreferenceRemoved = true;
                    return true;
                }
                return false;
            }
        };
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = AccessibilitySettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys = new ArrayList<>();

        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(mContext, R.xml.accessibility_settings));

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testAccessibilityShortcutPreference_byDefault_shouldBeShown() {
        final Preference preference = new Preference(mContext);
        mFragment.checkAccessibilityShortcutVisibility(preference);

        assertThat(mAccessibilityShortcutPreferenceRemoved).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testAccessibilityShortcutPreference_ifDisabled_shouldNotBeShown() {
        final Preference preference = new Preference(mContext);
        mFragment.checkAccessibilityShortcutVisibility(preference);

        assertThat(mAccessibilityShortcutPreferenceRemoved).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testNonIndexableKeys_ifAccessibilityShortcutNotVisible_containsKey() {
        final List<String> niks = AccessibilitySettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        assertThat(niks).contains(AccessibilitySettings.ACCESSIBILITY_SHORTCUT_PREFERENCE);
    }

    @Test
    public void testColorInversionPreference_byDefault_shouldBeShown() {
        final Preference preference = new Preference(mContext);
        mFragment.checkColorInversionVisibility(preference);

        assertThat(mColorInversionPreferenceRemoved).isEqualTo(false);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testColorInversionPreference_ifDisabled_shouldNotBeShown() {
        final Preference preference = new Preference(mContext);
        mFragment.checkColorInversionVisibility(preference);

        assertThat(mColorInversionPreferenceRemoved).isEqualTo(true);
    }

    @Test
    public void testColorCorrectionPreference_byDefault_shouldBeShown() {
        final Preference preference = new Preference(mContext);
        mFragment.checkColorCorrectionVisibility(preference);

        assertThat(mColorCorrectionPreferenceRemoved).isEqualTo(false);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testColorCorrectionPreference_ifDisabled_shouldNotBeShown() {
        final Preference preference = new Preference(mContext);
        mFragment.checkColorCorrectionVisibility(preference);

        assertThat(mColorCorrectionPreferenceRemoved).isEqualTo(true);
    }
}
