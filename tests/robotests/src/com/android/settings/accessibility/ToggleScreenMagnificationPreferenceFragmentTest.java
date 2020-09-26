/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.EXTRA_SHORTCUT_TYPE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
import com.android.settings.accessibility.ToggleFeaturePreferenceFragment.AccessibilityUserShortcutType;
import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class ToggleScreenMagnificationPreferenceFragmentTest {

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.mock.example";
    private static final String PLACEHOLDER_CLASS_NAME =
            PLACEHOLDER_PACKAGE_NAME + ".mock_a11y_service";
    private static final ComponentName PLACEHOLDER_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_CLASS_NAME);

    private static final String SOFTWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    private static final String HARDWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;
    private static final String TRIPLETAP_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED;

    private static final String MAGNIFICATION_CONTROLLER_NAME =
            "com.android.server.accessibility.MagnificationController";

    private TestToggleScreenMagnificationPreferenceFragment mFragment;
    private Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private FragmentActivity mActivity;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestToggleScreenMagnificationPreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        doReturn(null).when(mFragment).getPreferenceScreen();
        doReturn(mActivity).when(mFragment).getActivity();
    }

    @Test
    public void hasValueInSettings_putValue_hasValue() {
        setMagnificationTripleTapEnabled(/* enabled= */ true);

        assertThat(ToggleScreenMagnificationPreferenceFragment.hasMagnificationValuesInSettings(
                mContext, UserShortcutType.TRIPLETAP)).isTrue();
    }

    @Test
    public void optInAllValuesToSettings_optInValue_haveMatchString() {
        int shortcutTypes = UserShortcutType.SOFTWARE | UserShortcutType.TRIPLETAP;

        ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(mContext,
                shortcutTypes);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                MAGNIFICATION_CONTROLLER_NAME);
        assertThat(getMagnificationTripleTapStatus()).isTrue();

    }

    @Test
    public void optInAllValuesToSettings_existOtherValue_optInValue_haveMatchString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());

        ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(mContext,
                UserShortcutType.SOFTWARE);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                PLACEHOLDER_COMPONENT_NAME.flattenToString() + ":" + MAGNIFICATION_CONTROLLER_NAME);
    }

    @Test
    public void optOutAllValuesToSettings_optOutValue_emptyString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, MAGNIFICATION_CONTROLLER_NAME);
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY, MAGNIFICATION_CONTROLLER_NAME);
        setMagnificationTripleTapEnabled(/* enabled= */ true);
        int shortcutTypes =
                UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE | UserShortcutType.TRIPLETAP;

        ToggleScreenMagnificationPreferenceFragment.optOutAllMagnificationValuesFromSettings(
                mContext, shortcutTypes);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEmpty();
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEmpty();
        assertThat(getMagnificationTripleTapStatus()).isFalse();
    }

    @Test
    public void optOutValueFromSettings_existOtherValue_optOutValue_haveMatchString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY,
                PLACEHOLDER_COMPONENT_NAME.flattenToString() + ":" + MAGNIFICATION_CONTROLLER_NAME);
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY,
                PLACEHOLDER_COMPONENT_NAME.flattenToString() + ":" + MAGNIFICATION_CONTROLLER_NAME);
        int shortcutTypes = UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE;

        ToggleScreenMagnificationPreferenceFragment.optOutAllMagnificationValuesFromSettings(
                mContext, shortcutTypes);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                PLACEHOLDER_COMPONENT_NAME.flattenToString());
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEqualTo(
                PLACEHOLDER_COMPONENT_NAME.flattenToString());
    }

    @Test
    public void updateShortcutPreferenceData_assignDefaultValueToVariable() {
        mFragment.updateShortcutPreferenceData();

        // Compare to default UserShortcutType
        assertThat(mFragment.mUserShortcutTypes).isEqualTo(UserShortcutType.SOFTWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSettings_assignToVariable() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, MAGNIFICATION_CONTROLLER_NAME);
        setMagnificationTripleTapEnabled(/* enabled= */ true);
        mFragment.updateShortcutPreferenceData();

        assertThat(mFragment.mUserShortcutTypes).isEqualTo(
                UserShortcutType.SOFTWARE | UserShortcutType.TRIPLETAP);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSharedPreference_assignToVariable() {
        final AccessibilityUserShortcutType tripleTapShortcut = new AccessibilityUserShortcutType(
                MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.TRIPLETAP);

        putUserShortcutTypeIntoSharedPreference(mContext, tripleTapShortcut);
        mFragment.updateShortcutPreferenceData();

        assertThat(mFragment.mUserShortcutTypes).isEqualTo(UserShortcutType.TRIPLETAP);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void restoreValueFromSavedInstanceState_assignToVariable() {
        mContext.setTheme(R.style.Theme_AppCompat);
        final String dialogTitle = "title";
        final AlertDialog dialog = AccessibilityEditDialogUtils.showMagnificationEditShortcutDialog(
                mContext, dialogTitle, this::callEmptyOnClicked);
        final Bundle savedInstanceState = new Bundle();

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
            AccessibilityUserShortcutType shortcut) {
        Set<String> value = new HashSet<>(Collections.singletonList(shortcut.flattenToString()));

        SharedPreferenceUtils.setUserShortcutType(context, value);
    }

    private void setMagnificationTripleTapEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(), TRIPLETAP_SHORTCUT_KEY,
                enabled ? ON : OFF);
    }

    private String getStringFromSettings(String key) {
        return Settings.Secure.getString(mContext.getContentResolver(), key);
    }

    private boolean getMagnificationTripleTapStatus() {
        return Settings.Secure.getInt(mContext.getContentResolver(), TRIPLETAP_SHORTCUT_KEY, OFF)
                == ON;
    }

    private void callEmptyOnClicked(DialogInterface dialog, int which) {}

    public static class TestToggleScreenMagnificationPreferenceFragment
            extends ToggleScreenMagnificationPreferenceFragment {
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
