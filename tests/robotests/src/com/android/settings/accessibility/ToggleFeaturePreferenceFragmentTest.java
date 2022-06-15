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

import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.KEY_SAVED_USER_SHORTCUT_TYPE;

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

import androidx.annotation.XmlRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogType;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

/** Tests for {@link ToggleFeaturePreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
public class ToggleFeaturePreferenceFragmentTest {

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
    private static final String PLACEHOLDER_CLASS_NAME = PLACEHOLDER_PACKAGE_NAME + ".placeholder";
    private static final ComponentName PLACEHOLDER_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_CLASS_NAME);
    private static final String PLACEHOLDER_DIALOG_TITLE = "title";
    private static final String DEFAULT_SUMMARY = "default summary";
    private static final String DEFAULT_DESCRIPTION = "default description";

    private static final String SOFTWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    private static final String HARDWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;

    private TestToggleFeaturePreferenceFragment mFragment;
    private PreferenceScreen mScreen;
    private Context mContext = ApplicationProvider.getApplicationContext();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestToggleFeaturePreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        mScreen = spy(new PreferenceScreen(mContext, null));
        when(mScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
    }

    @Test
    public void createFragment_shouldOnlyAddPreferencesOnce() {
        FragmentController.setupFragment(mFragment, FragmentActivity.class,
                /* containerViewId= */ 0, /* bundle= */ null);

        // execute exactly once
        verify(mFragment).addPreferencesFromResource(R.xml.placeholder_prefs);
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
    public void restoreValueFromSavedInstanceState_assignToVariable() {
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
    public void createFooterPreference_shouldSetAsExpectedValue() {
        mFragment.createFooterPreference(mFragment.getPreferenceScreen(),
                DEFAULT_SUMMARY, DEFAULT_DESCRIPTION);

        AccessibilityFooterPreference accessibilityFooterPreference =
                (AccessibilityFooterPreference) mFragment.getPreferenceScreen().getPreference(
                        mFragment.getPreferenceScreen().getPreferenceCount() - 1);
        assertThat(accessibilityFooterPreference.getSummary()).isEqualTo(DEFAULT_SUMMARY);
        assertThat(accessibilityFooterPreference.isSelectable()).isEqualTo(true);
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

    public static class TestToggleFeaturePreferenceFragment
            extends ToggleFeaturePreferenceFragment {

        @Override
        protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        int getUserShortcutTypes() {
            return 0;
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

    }
}
