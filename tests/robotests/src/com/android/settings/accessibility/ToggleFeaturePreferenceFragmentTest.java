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

import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.EXTRA_SHORTCUT_TYPE;

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
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Tests for {@link ToggleFeaturePreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
public class ToggleFeaturePreferenceFragmentTest {

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
    private static final String PLACEHOLDER_CLASS_NAME = PLACEHOLDER_PACKAGE_NAME + ".placeholder";
    private static final ComponentName PLACEHOLDER_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_CLASS_NAME);

    private static final String SOFTWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    private static final String HARDWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;

    private TestToggleFeaturePreferenceFragment mFragment;
    private Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    private PreferenceManager mPreferenceManager;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestToggleFeaturePreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        doReturn(null).when(mFragment).getPreferenceScreen();
    }

    @Test
    public void createFragment_shouldOnlyAddPreferencesOnce() {
        FragmentController.setupFragment(mFragment, FragmentActivity.class,
                /* containerViewId= */ 0, /* bundle= */null);

        // execute exactly once
        verify(mFragment).addPreferencesFromResource(R.xml.placeholder_prefs);
    }

    @Test
    public void updateShortcutPreferenceData_assignDefaultValueToVariable() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;

        mFragment.updateShortcutPreferenceData();

        // Compare to default UserShortcutType
        assertThat(mFragment.mUserShortcutTypes).isEqualTo(UserShortcutType.SOFTWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSettings_assignToVariable() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;

        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());
        mFragment.updateShortcutPreferenceData();

        assertThat(mFragment.mUserShortcutTypes).isEqualTo(
                UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSharedPreference_assignToVariable() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
        final PreferredShortcut hardwareShortcut = new PreferredShortcut(
                PLACEHOLDER_COMPONENT_NAME.flattenToString(), UserShortcutType.HARDWARE);

        putUserShortcutTypeIntoSharedPreference(mContext, hardwareShortcut);
        mFragment.updateShortcutPreferenceData();

        assertThat(mFragment.mUserShortcutTypes).isEqualTo(UserShortcutType.HARDWARE);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void restoreValueFromSavedInstanceState_assignToVariable() {
        mContext.setTheme(R.style.Theme_AppCompat);
        final String dialogTitle = "title";
        final AlertDialog dialog = AccessibilityEditDialogUtils.showEditShortcutDialog(
                mContext, dialogTitle, this::callEmptyOnClicked);
        final Bundle savedInstanceState = new Bundle();
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;

        savedInstanceState.putInt(EXTRA_SHORTCUT_TYPE,
                UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE);
        mFragment.onCreate(savedInstanceState);
        mFragment.initializeDialogCheckBox(dialog);
        mFragment.updateUserShortcutType(true);

        assertThat(mFragment.mUserShortcutTypes).isEqualTo(
                UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE);

    }

    private void putStringIntoSettings(String key, String componentName) {
        Settings.Secure.putString(mContext.getContentResolver(), key, componentName);
    }

    private void putUserShortcutTypeIntoSharedPreference(Context context,
            PreferredShortcut shortcut) {
        Set<String> value = new HashSet<>(Collections.singletonList(shortcut.toString()));

        SharedPreferenceUtils.setUserShortcutType(context, value);
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
