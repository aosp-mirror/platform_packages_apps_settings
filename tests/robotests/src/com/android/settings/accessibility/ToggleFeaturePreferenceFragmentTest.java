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

import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.KEY_SAVED_QS_TOOLTIP_RESHOW;
import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.KEY_SAVED_USER_SHORTCUT_TYPE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.PopupWindow;

import androidx.annotation.XmlRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogType;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.widget.TopIntroPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.androidx.fragment.FragmentController;

/** Tests for {@link ToggleFeaturePreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
public class ToggleFeaturePreferenceFragmentTest {

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
    private static final String PLACEHOLDER_CLASS_NAME = PLACEHOLDER_PACKAGE_NAME + ".placeholder";
    private static final ComponentName PLACEHOLDER_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_CLASS_NAME);
    private static final String PLACEHOLDER_TILE_CLASS_NAME =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder";
    private static final ComponentName PLACEHOLDER_TILE_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME);
    private static final String PLACEHOLDER_TILE_TOOLTIP_CONTENT =
            PLACEHOLDER_PACKAGE_NAME + "tooltip_content";
    private static final String PLACEHOLDER_DIALOG_TITLE = "title";
    private static final String DEFAULT_SUMMARY = "default summary";
    private static final String DEFAULT_DESCRIPTION = "default description";
    private static final String DEFAULT_TOP_INTRO = "default top intro";

    private static final String SOFTWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    private static final String HARDWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;

    private TestToggleFeaturePreferenceFragment mFragment;
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    @Mock
    private FragmentActivity mActivity;
    @Mock
    private ContentResolver mContentResolver;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestToggleFeaturePreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getContentResolver()).thenReturn(mContentResolver);
        final PreferenceScreen screen = spy(new PreferenceScreen(mContext, null));
        when(screen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(screen).when(mFragment).getPreferenceScreen();
    }

    @Test
    public void createFragment_shouldOnlyAddPreferencesOnce() {
        FragmentController.setupFragment(mFragment, FragmentActivity.class,
                /* containerViewId= */ 0, /* bundle= */ null);

        // execute exactly once
        verify(mFragment).addPreferencesFromResource(R.xml.placeholder_prefs);
    }

    @Test
    @Config(shadows = {ShadowFragment.class})
    public void onResume_haveRegisterToSpecificUris() {
        mFragment.onAttach(mContext);
        mFragment.onCreate(Bundle.EMPTY);

        mFragment.onResume();

        verify(mContentResolver).registerContentObserver(
                eq(Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS)),
                eq(false),
                any(AccessibilitySettingsContentObserver.class));
        verify(mContentResolver).registerContentObserver(
                eq(Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE)),
                eq(false),
                any(AccessibilitySettingsContentObserver.class));
    }

    @Test
    public void updateShortcutPreferenceData_assignDefaultValueToVariable() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;

        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        // Compare to default UserShortcutType
        assertThat(expectedType).isEqualTo(UserShortcutType.SOFTWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSettings_assignToVariable() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());

        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        assertThat(expectedType).isEqualTo(UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSharedPreference_assignToVariable() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
        final PreferredShortcut hardwareShortcut = new PreferredShortcut(
                PLACEHOLDER_COMPONENT_NAME.flattenToString(), UserShortcutType.HARDWARE);

        putUserShortcutTypeIntoSharedPreference(mContext, hardwareShortcut);
        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        assertThat(expectedType).isEqualTo(UserShortcutType.HARDWARE);
    }

    @Test
    public void dialogCheckboxClicked_hardwareType_skipTimeoutRestriction() {
        mContext.setTheme(R.style.Theme_AppCompat);
        final ShortcutPreference shortcutPreference = new ShortcutPreference(mContext, /* attrs= */
                null);
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
        mFragment.mShortcutPreference = shortcutPreference;
        final AlertDialog dialog = AccessibilityDialogUtils.showEditShortcutDialog(
                mContext, DialogType.EDIT_SHORTCUT_GENERIC, PLACEHOLDER_DIALOG_TITLE,
                mFragment::callOnAlertDialogCheckboxClicked);
        mFragment.setupEditShortcutDialog(dialog);

        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        final CheckBox hardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        hardwareTypeCheckBox.setChecked(true);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
        final boolean skipTimeoutRestriction = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SKIP_ACCESSIBILITY_SHORTCUT_DIALOG_TIMEOUT_RESTRICTION, 0) != 0;

        assertThat(skipTimeoutRestriction).isTrue();
    }

    @Test
    public void setupEditShortcutDialog_shortcutPreferenceOff_checkboxIsEmptyValue() {
        mContext.setTheme(R.style.Theme_AppCompat);
        final AlertDialog dialog = AccessibilityDialogUtils.showEditShortcutDialog(
                mContext, DialogType.EDIT_SHORTCUT_GENERIC, PLACEHOLDER_DIALOG_TITLE,
                this::callEmptyOnClicked);
        final ShortcutPreference shortcutPreference = new ShortcutPreference(mContext, /* attrs= */
                null);
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
        mFragment.mShortcutPreference = shortcutPreference;

        mFragment.mShortcutPreference.setChecked(false);
        mFragment.setupEditShortcutDialog(dialog);

        final int checkboxValue = mFragment.getShortcutTypeCheckBoxValue();
        assertThat(checkboxValue).isEqualTo(UserShortcutType.EMPTY);
    }

    @Test
    public void setupEditShortcutDialog_shortcutPreferenceOn_checkboxIsSavedValue() {
        mContext.setTheme(R.style.Theme_AppCompat);
        final AlertDialog dialog = AccessibilityDialogUtils.showEditShortcutDialog(
                mContext, DialogType.EDIT_SHORTCUT_GENERIC, PLACEHOLDER_DIALOG_TITLE,
                this::callEmptyOnClicked);
        final ShortcutPreference shortcutPreference = new ShortcutPreference(mContext, /* attrs= */
                null);
        final PreferredShortcut hardwareShortcut = new PreferredShortcut(
                PLACEHOLDER_COMPONENT_NAME.flattenToString(), UserShortcutType.HARDWARE);
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
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
        mContext.setTheme(R.style.Theme_AppCompat);
        final AlertDialog dialog = AccessibilityDialogUtils.showEditShortcutDialog(
                mContext, DialogType.EDIT_SHORTCUT_GENERIC, PLACEHOLDER_DIALOG_TITLE,
                this::callEmptyOnClicked);
        final Bundle savedInstanceState = new Bundle();
        final ShortcutPreference shortcutPreference = new ShortcutPreference(mContext, /* attrs= */
                null);
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
        mFragment.mShortcutPreference = shortcutPreference;

        savedInstanceState.putInt(KEY_SAVED_USER_SHORTCUT_TYPE,
                UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE);
        mFragment.onCreate(savedInstanceState);
        mFragment.setupEditShortcutDialog(dialog);
        final int value = mFragment.getShortcutTypeCheckBoxValue();
        mFragment.saveNonEmptyUserShortcutType(value);

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        assertThat(expectedType).isEqualTo(UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onPreferenceToggledOnDisabledService_notShowTooltipView() {
        mContext.setTheme(R.style.Theme_AppCompat);

        mFragment.onPreferenceToggled(mFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ false);

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onPreferenceToggledOnEnabledService_showTooltipView() {
        mContext.setTheme(R.style.Theme_AppCompat);

        mFragment.onPreferenceToggled(mFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ true);

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onPreferenceToggledOnEnabledService_tooltipViewShown_notShowTooltipView() {
        mContext.setTheme(R.style.Theme_AppCompat);
        mFragment.onPreferenceToggled(mFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ true);
        getLatestPopupWindow().dismiss();

        mFragment.onPreferenceToggled(mFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ true);

        assertThat(getLatestPopupWindow().isShowing()).isFalse();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void restoreValueFromSavedInstanceState_showTooltipView() {
        mContext.setTheme(R.style.Theme_AppCompat);
        mFragment.onPreferenceToggled(mFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ true);
        assertThat(getLatestPopupWindow().isShowing()).isTrue();

        final Bundle savedInstanceState = new Bundle();
        savedInstanceState.putBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW, /* value= */ true);
        mFragment.onCreate(savedInstanceState);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onViewCreated(mFragment.getView(), savedInstanceState);
        mFragment.onAttach(mContext);

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    public void initTopIntroPreference_hasTopIntroTitle_shouldSetAsExpectedValue() {
        mFragment.mTopIntroTitle = DEFAULT_TOP_INTRO;
        mFragment.initTopIntroPreference();

        TopIntroPreference topIntroPreference =
                (TopIntroPreference) mFragment.getPreferenceScreen().getPreference(/* index= */ 0);
        assertThat(topIntroPreference.getTitle().toString()).isEqualTo(DEFAULT_TOP_INTRO);
    }

    @Test
    public void initTopIntroPreference_topIntroTitleIsNull_shouldNotAdded() {
        mFragment.initTopIntroPreference();

        assertThat(mFragment.getPreferenceScreen().getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void createFooterPreference_shouldSetAsExpectedValue() {
        mFragment.createFooterPreference(mFragment.getPreferenceScreen(),
                DEFAULT_SUMMARY, DEFAULT_DESCRIPTION);

        AccessibilityFooterPreference accessibilityFooterPreference =
                (AccessibilityFooterPreference) mFragment.getPreferenceScreen().getPreference(
                        mFragment.getPreferenceScreen().getPreferenceCount() - 1);
        assertThat(accessibilityFooterPreference.getSummary()).isEqualTo(DEFAULT_SUMMARY);
        assertThat(accessibilityFooterPreference.isSelectable()).isEqualTo(false);
        assertThat(accessibilityFooterPreference.getOrder()).isEqualTo(Integer.MAX_VALUE - 1);
    }

    private void putStringIntoSettings(String key, String componentName) {
        Settings.Secure.putString(mContext.getContentResolver(), key, componentName);
    }

    private void putUserShortcutTypeIntoSharedPreference(Context context,
            PreferredShortcut shortcut) {
        PreferredShortcuts.saveUserShortcutType(context, shortcut);
    }

    private void callEmptyOnClicked(DialogInterface dialog, int which) {}

    private static PopupWindow getLatestPopupWindow() {
        final ShadowApplication shadowApplication =
                Shadow.extract(ApplicationProvider.getApplicationContext());
        return shadowApplication.getLatestPopupWindow();
    }

    public static class TestToggleFeaturePreferenceFragment
            extends ToggleFeaturePreferenceFragment {

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        int getUserShortcutTypes() {
            return 0;
        }

        @Override
        ComponentName getTileComponentName() {
            return PLACEHOLDER_TILE_COMPONENT_NAME;
        }

        @Override
        protected CharSequence getTileTooltipContent(@QuickSettingsTooltipType int type) {
            return PLACEHOLDER_TILE_TOOLTIP_CONTENT;
        }

        @Override
        public int getPreferenceScreenResId() {
            return R.xml.placeholder_prefs;
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
        public void onDestroyView() {
            // do nothing
        }

        @Override
        public void addPreferencesFromResource(@XmlRes int preferencesResId) {
            // do nothing
        }

        @Override
        protected void updateShortcutPreference() {
            // UI related function, do nothing in tests
        }

        @Override
        public View getView() {
            return mock(View.class);
        }
    }
}
