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

import static android.provider.Settings.Secure.NAVIGATION_MODE;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Flags;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowAccessibilityManager;
import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link AccessibilityButtonPreferenceController}. */
@Config(shadows = {
        SettingsShadowResources.class,
        com.android.settings.testutils.shadow.ShadowAccessibilityManager.class
})
@RunWith(RobolectricTestRunner.class)
public class AccessibilityButtonPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private final Resources mResources = mContext.getResources();
    @Mock
    private PreferenceScreen mScreen;
    private Preference mPreference;
    private AccessibilityButtonPreferenceController mController;
    private ShadowAccessibilityManager mShadowAccessibilityManager;

    @Before
    public void setUp() {
        mShadowAccessibilityManager = Shadow.extract(
                mContext.getSystemService(AccessibilityManager.class));
        mController = new AccessibilityButtonPreferenceController(mContext, "test_key");
        mPreference = new Preference(mContext);
        mPreference.setKey("test_key");

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mContext.getResources()).thenReturn(mResources);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void displayPreference_navigationGestureEnabled_setCorrectTitle() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                NAVIGATION_MODE, NAV_BAR_MODE_GESTURAL, mContext.getUserId());

        mController.displayPreference(mScreen);

        assertThat(mPreference.getTitle()).isEqualTo(
                mContext.getText(R.string.accessibility_button_gesture_title));
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void displayPreference_navigationGestureEnabled_flag_setCorrectTitle() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                NAVIGATION_MODE, NAV_BAR_MODE_GESTURAL, mContext.getUserId());

        mController.displayPreference(mScreen);

        assertThat(mPreference.getTitle()).isEqualTo(
                mContext.getText(R.string.accessibility_button_title));
    }

    @Test
    public void displayPreference_navigationGestureDisabled_setCorrectTitle() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                NAVIGATION_MODE, NAV_BAR_MODE_2BUTTON, mContext.getUserId());

        mController.displayPreference(mScreen);

        assertThat(mPreference.getTitle()).isEqualTo(
                mContext.getText(R.string.accessibility_button_title));
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void updateDynamicRawDataToIndex_navigationGestureEnabled_setCorrectIndex() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                NAVIGATION_MODE, NAV_BAR_MODE_GESTURAL, mContext.getUserId());
        List<SearchIndexableRaw> rawDataList = new ArrayList<>();

        mController.updateDynamicRawDataToIndex(rawDataList);

        assertThat(rawDataList).hasSize(1);
        SearchIndexableRaw raw = rawDataList.get(0);
        assertThat(raw.title).isEqualTo(
                mResources.getString(R.string.accessibility_button_gesture_title));
        assertThat(raw.screenTitle).isEqualTo(
                mResources.getString(R.string.accessibility_shortcuts_settings_title));
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void updateDynamicRawDataToIndex_navigationGestureEnabled_flag_setCorrectIndex() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                NAVIGATION_MODE, NAV_BAR_MODE_GESTURAL, mContext.getUserId());
        List<SearchIndexableRaw> rawDataList = new ArrayList<>();

        mController.updateDynamicRawDataToIndex(rawDataList);

        assertThat(rawDataList).hasSize(1);
        SearchIndexableRaw raw = rawDataList.get(0);
        assertThat(raw.title).isEqualTo(
                mResources.getString(R.string.accessibility_button_title));
        assertThat(raw.screenTitle).isEqualTo(
                mResources.getString(R.string.accessibility_shortcuts_settings_title));
    }

    @Test
    public void updateDynamicRawDataToIndex_navigationGestureDisabled_setCorrectIndex() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                NAVIGATION_MODE, NAV_BAR_MODE_2BUTTON, mContext.getUserId());
        List<SearchIndexableRaw> rawDataList = new ArrayList<>();

        mController.updateDynamicRawDataToIndex(rawDataList);

        assertThat(rawDataList).hasSize(1);
        SearchIndexableRaw raw = rawDataList.get(0);
        assertThat(raw.title).isEqualTo(
                mResources.getString(R.string.accessibility_button_title));
        assertThat(raw.screenTitle).isEqualTo(
                mResources.getString(R.string.accessibility_shortcuts_settings_title));
    }

    @Test
    @EnableFlags(com.android.settings.accessibility.Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getAvailabilityStatus_settingEmpty_disabled() {
        mShadowAccessibilityManager.setAccessibilityShortcutTargets(SOFTWARE, List.of());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @EnableFlags(com.android.settings.accessibility.Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getAvailabilityStatus_settingNotEmpty_available() {
        mShadowAccessibilityManager.setAccessibilityShortcutTargets(SOFTWARE, List.of("Foo"));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
