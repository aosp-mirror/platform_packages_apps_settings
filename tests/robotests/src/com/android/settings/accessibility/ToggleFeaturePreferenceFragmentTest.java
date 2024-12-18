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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.CaseMap;
import android.os.Bundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.widget.TopIntroPreference;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.Locale;

/** Tests for {@link ToggleFeaturePreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowFragment.class,
})
public class ToggleFeaturePreferenceFragmentTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

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
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    @Mock
    private FragmentActivity mActivity;
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestToggleFeaturePreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getContentResolver()).thenReturn(mContentResolver);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        final PreferenceScreen screen = spy(new PreferenceScreen(mContext, null));
        when(screen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(screen).when(mFragment).getPreferenceScreen();
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
    }

    @Test
    public void getPreferenceScreenResId_returnsExpectedPreferenceScreenResId() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(R.xml.placeholder_prefs);
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    @Config(shadows = {ShadowFragment.class})
    public void onResume_flagEnabled_haveRegisterToSpecificUris() {
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
        verify(mContentResolver).registerContentObserver(
                eq(Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_QS_TARGETS)),
                eq(false),
                any(AccessibilitySettingsContentObserver.class));
    }

    @Test
    @DisableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    @Config(shadows = {ShadowFragment.class})
    public void onResume_flagDisabled_haveRegisterToSpecificUris() {
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
        verify(mContentResolver, never()).registerContentObserver(
                eq(Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_QS_TARGETS)),
                eq(false),
                any(AccessibilitySettingsContentObserver.class));
    }

    @Test
    public void updateShortcutPreferenceData_assignDefaultValueToVariable() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;

        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.mComponentName.flattenToString());
        // Compare to default UserShortcutType
        assertThat(expectedType).isEqualTo(SOFTWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSettings_assignToVariable() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
        putSecureStringIntoSettings(SOFTWARE_SHORTCUT_KEY,
                PLACEHOLDER_COMPONENT_NAME.flattenToString());
        putSecureStringIntoSettings(HARDWARE_SHORTCUT_KEY,
                PLACEHOLDER_COMPONENT_NAME.flattenToString());

        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.mComponentName.flattenToString());
        assertThat(expectedType).isEqualTo(SOFTWARE | HARDWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSharedPreference_assignToVariable() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
        final PreferredShortcut hardwareShortcut = new PreferredShortcut(
                PLACEHOLDER_COMPONENT_NAME.flattenToString(), HARDWARE);

        putUserShortcutTypeIntoSharedPreference(mContext, hardwareShortcut);
        mFragment.updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                mFragment.mComponentName.flattenToString());
        assertThat(expectedType).isEqualTo(HARDWARE);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onPreferenceToggledOnDisabledService_notShowTooltipView() {
        mFragment.onPreferenceToggled(
                ToggleFeaturePreferenceFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ false);

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    @DisableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    @Config(shadows = ShadowFragment.class)
    public void onPreferenceToggledOnEnabledService_showTooltipView() {
        mFragment.onPreferenceToggled(
                ToggleFeaturePreferenceFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ true);

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onPreferenceToggledOnEnabledService_inSuw_toolTipViewShouldNotShow() {
        Intent suwIntent = new Intent();
        suwIntent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
        when(mActivity.getIntent()).thenReturn(suwIntent);

        mFragment.onPreferenceToggled(
                ToggleFeaturePreferenceFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ true);

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    @DisableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    @Config(shadows = ShadowFragment.class)
    public void onPreferenceToggledOnEnabledService_tooltipViewShown_notShowTooltipView() {
        mFragment.onPreferenceToggled(
                ToggleFeaturePreferenceFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ true);
        getLatestPopupWindow().dismiss();

        mFragment.onPreferenceToggled(
                ToggleFeaturePreferenceFragment.KEY_USE_SERVICE_PREFERENCE, /* enabled= */ true);

        assertThat(getLatestPopupWindow().isShowing()).isFalse();
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
    @EnableFlags(Flags.FLAG_ACCESSIBILITY_SHOW_APP_INFO_BUTTON)
    public void createAppInfoPreference_withValidComponentName() {
        when(mPackageManager.isPackageAvailable(PLACEHOLDER_PACKAGE_NAME)).thenReturn(true);
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;

        final Preference preference = mFragment.createAppInfoPreference();

        assertThat(preference).isNotNull();
        final Intent appInfoIntent = preference.getIntent();
        assertThat(appInfoIntent.getAction())
                .isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        assertThat(appInfoIntent.getDataString()).isEqualTo("package:" + PLACEHOLDER_PACKAGE_NAME);
    }

    @Test
    @EnableFlags(Flags.FLAG_ACCESSIBILITY_SHOW_APP_INFO_BUTTON)
    public void createAppInfoPreference_noComponentName_shouldBeNull() {
        mFragment.mComponentName = null;

        final Preference preference = mFragment.createAppInfoPreference();

        assertThat(preference).isNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_ACCESSIBILITY_SHOW_APP_INFO_BUTTON)
    public void createAppInfoPreference_withUnavailablePackage_shouldBeNull() {
        when(mPackageManager.isPackageAvailable(PLACEHOLDER_PACKAGE_NAME)).thenReturn(false);
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;

        final Preference preference = mFragment.createAppInfoPreference();

        assertThat(preference).isNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_ACCESSIBILITY_SHOW_APP_INFO_BUTTON)
    public void createAppInfoPreference_inSetupWizard_shouldBeNull() {
        when(mFragment.isAnySetupWizard()).thenReturn(true);
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;

        final Preference preference = mFragment.createAppInfoPreference();

        assertThat(preference).isNull();
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

    @Test
    @Config(shadows = ShadowFragment.class)
    public void writeConfigDefaultIfNeeded_sameCNWithFragAndConfig_SameValueInVolumeSettingsKey() {
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
        doReturn(PLACEHOLDER_COMPONENT_NAME.flattenToString()).when(mFragment).getString(
                com.android.internal.R.string.config_defaultAccessibilityService);

        mFragment.writeConfigDefaultAccessibilityServiceIntoShortcutTargetServiceIfNeeded(mContext);

        assertThat(
                getSecureStringFromSettings(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE))
                .isEqualTo(PLACEHOLDER_COMPONENT_NAME.flattenToString());
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    @Config(shadows = ShadowFragment.class)
    public void showQuickSettingsTooltipIfNeeded_qsFlagOn_dontShowTooltipView() {
        mFragment.showQuickSettingsTooltipIfNeeded(QuickSettingsTooltipType.GUIDE_TO_EDIT);

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    public void getShortcutTypeSummary_shortcutSummaryIsCorrectlySet() {
        final PreferredShortcut userPreferredShortcut = new PreferredShortcut(
                PLACEHOLDER_COMPONENT_NAME.flattenToString(),
                HARDWARE | QUICK_SETTINGS);
        putUserShortcutTypeIntoSharedPreference(mContext, userPreferredShortcut);
        final ShortcutPreference shortcutPreference =
                new ShortcutPreference(mContext, /* attrs= */ null);
        shortcutPreference.setChecked(true);
        shortcutPreference.setSettingsEditable(true);
        mFragment.mComponentName = PLACEHOLDER_COMPONENT_NAME;
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

    private void putSecureStringIntoSettings(String key, String componentName) {
        Settings.Secure.putString(mContext.getContentResolver(), key, componentName);
    }

    private String getSecureStringFromSettings(String key) {
        return Settings.Secure.getString(mContext.getContentResolver(), key);
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
        protected String getLogTag() {
            return null;
        }

        @Override
        protected void onProcessArguments(Bundle arguments) {
            // do nothing
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // do nothing
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

        @SuppressWarnings("MissingSuperCall")
        @Override
        public void onDestroyView() {
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
