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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.settings.accessibility.AccessibilityShortcutPreferenceFragment.KEY_SAVED_QS_TOOLTIP_RESHOW;
import static com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.icu.text.CaseMap;
import android.os.Bundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.Flags;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment;
import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.Locale;

/** Tests for {@link AccessibilityShortcutPreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class AccessibilityShortcutPreferenceFragmentTest {
    private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
    private static final String PLACEHOLDER_CLASS_NAME = PLACEHOLDER_PACKAGE_NAME + ".placeholder";
    private static final String PLACEHOLDER_TILE_CLASS_NAME =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder";
    private static final ComponentName PLACEHOLDER_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_CLASS_NAME);
    private static final ComponentName PLACEHOLDER_TILE_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME);
    private static final String PLACEHOLDER_TILE_TOOLTIP_CONTENT =
            PLACEHOLDER_PACKAGE_NAME + "tooltip_content";
    private static final String PLACEHOLDER_DIALOG_TITLE = "title";

    private static final String SOFTWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    private static final String HARDWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private TestAccessibilityShortcutPreferenceFragment mFragment;
    private PreferenceScreen mScreen;
    private Context mContext = ApplicationProvider.getApplicationContext();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestAccessibilityShortcutPreferenceFragment(null));
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(Robolectric.setupActivity(FragmentActivity.class));
        mScreen = spy(new PreferenceScreen(mContext, null));
        when(mScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
    }

    @Test
    public void updateShortcutPreferenceData_assignDefaultValueToVariable() {
        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.getComponentName().flattenToString());
        // Compare to default UserShortcutType
        assertThat(expectedType).isEqualTo(SOFTWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSettings_assignToVariable() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());

        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.getComponentName().flattenToString());
        assertThat(expectedType).isEqualTo(SOFTWARE | HARDWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSharedPreference_assignToVariable() {
        final PreferredShortcut hardwareShortcut = new PreferredShortcut(
                PLACEHOLDER_COMPONENT_NAME.flattenToString(), HARDWARE);

        putUserShortcutTypeIntoSharedPreference(mContext, hardwareShortcut);
        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.getComponentName().flattenToString());
        assertThat(expectedType).isEqualTo(HARDWARE);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    @Config(shadows = ShadowFragment.class)
    public void restoreValueFromSavedInstanceState_showTooltipView() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mFragment.showQuickSettingsTooltipIfNeeded(QuickSettingsTooltipType.GUIDE_TO_EDIT);
        assertThat(getLatestPopupWindow().isShowing()).isTrue();

        final Bundle savedInstanceState = new Bundle();
        savedInstanceState.putBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW, /* value= */ true);
        mFragment.onAttach(mContext);
        mFragment.onCreate(savedInstanceState);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onViewCreated(mFragment.getView(), savedInstanceState);

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    @Config(shadows = ShadowFragment.class)
    public void showQuickSettingsTooltipIfNeeded_qsFlagOn_dontShowTooltipView() {
        mFragment.showQuickSettingsTooltipIfNeeded(QuickSettingsTooltipType.GUIDE_TO_EDIT);

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void showGeneralCategory_shouldInitCategory() {
        final Bundle savedInstanceState = new Bundle();
        when(mFragment.showGeneralCategory()).thenReturn(true);
        mFragment.onAttach(mContext);
        mFragment.onCreate(savedInstanceState);

        verify(mFragment).initGeneralCategory();
    }

    @Test
    public void showGeneralCategory_shouldSetDefaultDescription() {
        assertThat(mFragment.getGeneralCategoryDescription(null)).isNotNull();
    }

    @Test
    public void onSettingsClicked_showFullScreenEditShortcutScreen() {
        Activity activity = Robolectric.setupActivity(FragmentActivity.class);
        when(mFragment.getContext()).thenReturn(activity);
        Context context = mFragment.getContext();
        final ShortcutPreference shortcutPreference =
                new ShortcutPreference(context, /* attrs= */ null);

        mFragment.onSettingsClicked(shortcutPreference);

        Intent intent = shadowOf(
                (Application) context.getApplicationContext()).getNextStartedActivity();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent()).isEqualTo(
                new ComponentName(context, SubSettings.class));
        assertThat(intent.getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(EditShortcutsPreferenceFragment.class.getName());
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void getShortcutTypeSummary_shortcutSummaryIsCorrectlySet() {
        final PreferredShortcut userPreferredShortcut = new PreferredShortcut(
                PLACEHOLDER_COMPONENT_NAME.flattenToString(),
                HARDWARE | QUICK_SETTINGS);
        putUserShortcutTypeIntoSharedPreference(mContext, userPreferredShortcut);
        final ShortcutPreference shortcutPreference =
                new ShortcutPreference(mContext, /* attrs= */ null);
        shortcutPreference.setChecked(true);
        shortcutPreference.setSettingsEditable(true);
        mFragment.mShortcutPreference = shortcutPreference;
        String expected = CaseMap.toTitle().wholeString().noLowercase().apply(Locale.getDefault(),
                /* iter= */ null,
                mContext.getString(
                        R.string.accessibility_feature_shortcut_setting_summary_quick_settings)
                        + ", "
                        + mContext.getString(R.string.accessibility_shortcut_hardware_keyword));

        String summary = mFragment.getShortcutTypeSummary(mContext).toString();
        assertThat(summary).isEqualTo(expected);
    }

    private void putStringIntoSettings(String key, String componentName) {
        Settings.Secure.putString(mContext.getContentResolver(), key, componentName);
    }

    private void putUserShortcutTypeIntoSharedPreference(Context context,
            PreferredShortcut shortcut) {
        PreferredShortcuts.saveUserShortcutType(context, shortcut);
    }

    private static PopupWindow getLatestPopupWindow() {
        final ShadowApplication shadowApplication =
                Shadow.extract(ApplicationProvider.getApplicationContext());
        return shadowApplication.getLatestPopupWindow();
    }

    public static class TestAccessibilityShortcutPreferenceFragment
            extends AccessibilityShortcutPreferenceFragment {

        public TestAccessibilityShortcutPreferenceFragment(String restrictionKey) {
            super(restrictionKey);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return mock(View.class);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // do nothing
        }

        @Override
        protected ComponentName getComponentName() {
            return PLACEHOLDER_COMPONENT_NAME;
        }

        @Override
        protected CharSequence getLabelName() {
            return PLACEHOLDER_PACKAGE_NAME;
        }

        @Override
        protected ComponentName getTileComponentName() {
            return PLACEHOLDER_TILE_COMPONENT_NAME;
        }

        @Override
        protected CharSequence getTileTooltipContent(@QuickSettingsTooltipType int type) {
            return PLACEHOLDER_TILE_TOOLTIP_CONTENT;
        }

        @Override
        public int getUserShortcutTypes() {
            return 0;
        }

        @Override
        protected CharSequence getGeneralCategoryDescription(@Nullable CharSequence title) {
            return super.getGeneralCategoryDescription(null);
        }

        @Override
        protected boolean showGeneralCategory() {
            // For showGeneralCategory_shouldInitCategory()
            return true;
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        protected int getPreferenceScreenResId() {
            return 0;
        }

        @Override
        protected String getLogTag() {
            return null;
        }

        @Override
        public View getView() {
            return mock(View.class);
        }
    };
}
