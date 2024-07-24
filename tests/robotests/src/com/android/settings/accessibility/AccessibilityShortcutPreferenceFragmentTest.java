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

import static com.android.settings.accessibility.AccessibilityShortcutPreferenceFragment.KEY_SAVED_QS_TOOLTIP_RESHOW;
import static com.android.settings.accessibility.AccessibilityShortcutPreferenceFragment.KEY_SAVED_USER_SHORTCUT_TYPE;
import static com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import static com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
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
        assertThat(expectedType).isEqualTo(UserShortcutType.SOFTWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSettings_assignToVariable() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());

        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.getComponentName().flattenToString());
        assertThat(expectedType).isEqualTo(UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSharedPreference_assignToVariable() {
        final PreferredShortcut hardwareShortcut = new PreferredShortcut(
                PLACEHOLDER_COMPONENT_NAME.flattenToString(), UserShortcutType.HARDWARE);

        putUserShortcutTypeIntoSharedPreference(mContext, hardwareShortcut);
        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.getComponentName().flattenToString());
        assertThat(expectedType).isEqualTo(UserShortcutType.HARDWARE);
    }

    @Test
    public void setupEditShortcutDialog_shortcutPreferenceOff_checkboxIsEmptyValue() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        final AlertDialog dialog = AccessibilityDialogUtils.showEditShortcutDialog(
                mContext, AccessibilityDialogUtils.DialogType.EDIT_SHORTCUT_GENERIC,
                PLACEHOLDER_DIALOG_TITLE,
                this::callEmptyOnClicked);
        final ShortcutPreference shortcutPreference = new ShortcutPreference(mContext, /* attrs= */
                null);
        mFragment.mShortcutPreference = shortcutPreference;

        mFragment.mShortcutPreference.setChecked(false);
        mFragment.setupEditShortcutDialog(dialog);

        final int checkboxValue = mFragment.getShortcutTypeCheckBoxValue();
        assertThat(checkboxValue).isEqualTo(UserShortcutType.EMPTY);
    }

    @Test
    public void setupEditShortcutDialog_shortcutPreferenceOn_checkboxIsSavedValue() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        final AlertDialog dialog = AccessibilityDialogUtils.showEditShortcutDialog(
                mContext, AccessibilityDialogUtils.DialogType.EDIT_SHORTCUT_GENERIC,
                PLACEHOLDER_DIALOG_TITLE,
                this::callEmptyOnClicked);
        final ShortcutPreference shortcutPreference = new ShortcutPreference(mContext, /* attrs= */
                null);
        final PreferredShortcut hardwareShortcut = new PreferredShortcut(
                PLACEHOLDER_COMPONENT_NAME.flattenToString(), UserShortcutType.HARDWARE);
        mFragment.mShortcutPreference = shortcutPreference;

        PreferredShortcuts.saveUserShortcutType(mContext, hardwareShortcut);
        mFragment.mShortcutPreference.setChecked(true);
        mFragment.setupEditShortcutDialog(dialog);

        final int checkboxValue = mFragment.getShortcutTypeCheckBoxValue();
        assertThat(checkboxValue).isEqualTo(UserShortcutType.HARDWARE);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void restoreValueFromSavedInstanceState_assignShortcutTypeToVariable() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        final AlertDialog dialog = AccessibilityDialogUtils.showEditShortcutDialog(
                mContext, AccessibilityDialogUtils.DialogType.EDIT_SHORTCUT_GENERIC,
                PLACEHOLDER_DIALOG_TITLE,
                this::callEmptyOnClicked);
        final Bundle savedInstanceState = new Bundle();
        final ShortcutPreference shortcutPreference = new ShortcutPreference(mContext, /* attrs= */
                null);
        mFragment.mShortcutPreference = shortcutPreference;

        savedInstanceState.putInt(KEY_SAVED_USER_SHORTCUT_TYPE,
                UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE);
        mFragment.onAttach(mContext);
        mFragment.onCreate(savedInstanceState);
        mFragment.setupEditShortcutDialog(dialog);
        final int value = mFragment.getShortcutTypeCheckBoxValue();
        mFragment.saveNonEmptyUserShortcutType(value);

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.getComponentName().flattenToString());
        assertThat(expectedType).isEqualTo(UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE);
    }

    @Test
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

    private void callEmptyOnClicked(DialogInterface dialog, int which) {}

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
