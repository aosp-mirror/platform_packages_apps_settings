/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.service.quicksettings.TileService;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.Flags;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment;
import com.android.settings.widget.SettingsMainSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.List;

/** Tests for {@link ToggleAccessibilityServicePreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
public class ToggleAccessibilityServicePreferenceFragmentTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
    private static final String PLACEHOLDER_PACKAGE_NAME2 = "com.placeholder.example2";
    private static final String PLACEHOLDER_SERVICE_CLASS_NAME = "a11yservice1";
    private static final String PLACEHOLDER_SERVICE_CLASS_NAME2 = "a11yservice2";
    private static final String PLACEHOLDER_TILE_CLASS_NAME =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder";
    private static final String PLACEHOLDER_TILE_CLASS_NAME2 =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder2";
    private static final ComponentName PLACEHOLDER_TILE_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME);
    private static final String PLACEHOLDER_TILE_NAME =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder";
    private static final String PLACEHOLDER_TILE_NAME2 =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder2";
    private static final int NO_DIALOG = -1;

    private TestToggleAccessibilityServicePreferenceFragment mFragment;
    private PreferenceScreen mScreen;
    private Context mContext;

    private ShadowAccessibilityManager mShadowAccessibilityManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    @Mock
    private AccessibilityManager mMockAccessibilityManager;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mFragment = spy(new TestToggleAccessibilityServicePreferenceFragment());
        mFragment.setArguments(new Bundle());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        mScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));
        when(mScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
        mShadowAccessibilityManager = Shadow.extract(AccessibilityManager.getInstance(mContext));
    }

    @Test
    public void getTileTooltipContent_noTileServiceAssigned_returnNull() {
        final CharSequence tileTooltipContent =
                mFragment.getTileTooltipContent(QuickSettingsTooltipType.GUIDE_TO_EDIT);

        assertThat(tileTooltipContent).isNull();
    }

    @Test
    public void getTileTooltipContent_hasOneTileService_guideToEdit_haveMatchString() {
        setupTileService(PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME,
                PLACEHOLDER_TILE_NAME);

        final CharSequence tileTooltipContent =
                mFragment.getTileTooltipContent(QuickSettingsTooltipType.GUIDE_TO_EDIT);
        final CharSequence tileName =
                mFragment.loadTileLabel(mContext, mFragment.getTileComponentName());
        assertThat(tileTooltipContent.toString()).isEqualTo(
                mContext.getString(R.string.accessibility_service_qs_tooltip_content, tileName));
    }

    @Test
    public void getTileTooltipContent_hasOneTileService_guideToDirectUse_haveMatchString() {
        setupTileService(PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME,
                PLACEHOLDER_TILE_NAME);

        final CharSequence tileTooltipContent =
                mFragment.getTileTooltipContent(QuickSettingsTooltipType.GUIDE_TO_DIRECT_USE);
        final CharSequence tileName =
                mFragment.loadTileLabel(mContext, mFragment.getTileComponentName());
        assertThat(tileTooltipContent.toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_service_auto_added_qs_tooltip_content, tileName));
    }

    @Test
    public void getTileTooltipContent_hasTwoTileServices_guideToEdit_haveMatchString() {
        setupTileService(PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME,
                PLACEHOLDER_TILE_NAME);
        setupTileService(PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME2,
                PLACEHOLDER_TILE_NAME2);

        final CharSequence tileTooltipContent =
                mFragment.getTileTooltipContent(QuickSettingsTooltipType.GUIDE_TO_EDIT);
        final CharSequence tileName =
                mFragment.loadTileLabel(mContext, mFragment.getTileComponentName());
        assertThat(tileTooltipContent.toString()).isEqualTo(
                mContext.getString(R.string.accessibility_service_qs_tooltip_content, tileName));
    }

    @Test
    public void getTileTooltipContent_hasTwoTileServices_guideToDirectUse_haveMatchString() {
        setupTileService(PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME,
                PLACEHOLDER_TILE_NAME);
        setupTileService(PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME2,
                PLACEHOLDER_TILE_NAME2);

        final CharSequence tileTooltipContent =
                mFragment.getTileTooltipContent(QuickSettingsTooltipType.GUIDE_TO_DIRECT_USE);
        final CharSequence tileName =
                mFragment.loadTileLabel(mContext, mFragment.getTileComponentName());
        assertThat(tileTooltipContent.toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_service_auto_added_qs_tooltip_content, tileName));
    }

    @Test
    public void getAccessibilityServiceInfo() throws Throwable {
        final AccessibilityServiceInfo info1 = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        final AccessibilityServiceInfo info2 = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME2);
        final AccessibilityServiceInfo info3 = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME2,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        final AccessibilityServiceInfo info4 = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME2,
                PLACEHOLDER_SERVICE_CLASS_NAME2);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                List.of(info1, info2, info3, info4));

        mFragment.mComponentName = info3.getComponentName();

        assertThat(mFragment.getAccessibilityServiceInfo()).isEqualTo(info3);
    }

    @Test
    public void getAccessibilityServiceInfo_notFound() throws Throwable {
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of());

        mFragment.mComponentName = getFakeAccessibilityServiceInfo(PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME).getComponentName();

        assertThat(mFragment.getAccessibilityServiceInfo()).isNull();
    }

    @Test
    public void serviceSupportsAccessibilityButton() throws Throwable {
        final AccessibilityServiceInfo infoWithA11yButton = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        infoWithA11yButton.flags = AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
        final AccessibilityServiceInfo infoWithoutA11yButton = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME2,
                PLACEHOLDER_SERVICE_CLASS_NAME2);
        infoWithoutA11yButton.flags = 0;
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                List.of(infoWithA11yButton, infoWithoutA11yButton));

        mFragment.mComponentName = infoWithA11yButton.getComponentName();
        assertThat(mFragment.serviceSupportsAccessibilityButton()).isTrue();
        mFragment.mComponentName = infoWithoutA11yButton.getComponentName();
        assertThat(mFragment.serviceSupportsAccessibilityButton()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_CLEANUP_ACCESSIBILITY_WARNING_DIALOG)
    public void enableService_warningRequired_showWarning() throws Throwable {
        setupServiceWarningRequired(true);
        mFragment.mToggleServiceSwitchPreference =
                new SettingsMainSwitchPreference(mContext, /* attrs= */null);

        mFragment.onCheckedChanged(null, true);

        assertThat(mFragment.mLastShownDialogId).isEqualTo(
                AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_TOGGLE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_CLEANUP_ACCESSIBILITY_WARNING_DIALOG)
    public void enableService_warningNotRequired_dontShowWarning() throws Throwable {
        final AccessibilityServiceInfo info = setupServiceWarningRequired(false);
        mFragment.mToggleServiceSwitchPreference =
                new SettingsMainSwitchPreference(mContext, /* attrs= */null);
        mFragment.mPreferenceKey = info.getComponentName().flattenToString();

        mFragment.onCheckedChanged(null, true);

        assertThat(mFragment.mLastShownDialogId).isEqualTo(
                AccessibilityDialogUtils.DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_CLEANUP_ACCESSIBILITY_WARNING_DIALOG)
    public void toggleShortcutPreference_warningRequired_showWarning() throws Throwable {
        setupServiceWarningRequired(true);
        mFragment.mShortcutPreference = new ShortcutPreference(mContext, /* attrs= */null);

        mFragment.mShortcutPreference.setChecked(true);
        mFragment.onToggleClicked(mFragment.mShortcutPreference);

        assertThat(mFragment.mLastShownDialogId).isEqualTo(
                AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_SHORTCUT_TOGGLE);
        assertThat(mFragment.mShortcutPreference.isChecked()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_CLEANUP_ACCESSIBILITY_WARNING_DIALOG)
    public void toggleShortcutPreference_warningNotRequired_dontShowWarning() throws Throwable {
        setupServiceWarningRequired(false);
        mFragment.mShortcutPreference = new ShortcutPreference(mContext, /* attrs= */null);

        mFragment.mShortcutPreference.setChecked(true);
        mFragment.onToggleClicked(mFragment.mShortcutPreference);

        assertThat(mFragment.mLastShownDialogId).isEqualTo(
                AccessibilityDialogUtils.DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        assertThat(mFragment.mShortcutPreference.isChecked()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_CLEANUP_ACCESSIBILITY_WARNING_DIALOG)
    public void clickShortcutSettingsPreference_warningRequired_showWarning() throws Throwable {
        setupServiceWarningRequired(true);
        mFragment.mShortcutPreference = new ShortcutPreference(mContext, /* attrs= */null);

        mFragment.onSettingsClicked(mFragment.mShortcutPreference);

        assertThat(mFragment.mLastShownDialogId).isEqualTo(
                AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_SHORTCUT);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_CLEANUP_ACCESSIBILITY_WARNING_DIALOG)
    @RequiresFlagsDisabled(
            com.android.settings.accessibility.Flags.FLAG_EDIT_SHORTCUTS_IN_FULL_SCREEN)
    public void clickShortcutSettingsPreference_warningNotRequired_dontShowWarning_showDialog()
            throws Throwable {
        setupServiceWarningRequired(false);
        mFragment.mShortcutPreference = new ShortcutPreference(mContext, /* attrs= */null);

        mFragment.onSettingsClicked(mFragment.mShortcutPreference);

        assertThat(mFragment.mLastShownDialogId).isEqualTo(
                AccessibilityDialogUtils.DialogEnums.EDIT_SHORTCUT);
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_CLEANUP_ACCESSIBILITY_WARNING_DIALOG,
            com.android.settings.accessibility.Flags.FLAG_EDIT_SHORTCUTS_IN_FULL_SCREEN})
    public void clickShortcutSettingsPreference_warningNotRequired_dontShowWarning_launchActivity()
            throws Throwable {
        setupServiceWarningRequired(false);
        mFragment.mShortcutPreference = new ShortcutPreference(mContext, /* attrs= */null);
        doNothing().when(mContext).startActivity(any());

        mFragment.onSettingsClicked(mFragment.mShortcutPreference);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(captor.capture());
        assertThat(captor.getValue().getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(EditShortcutsPreferenceFragment.class.getName());
    }

    private void setupTileService(String packageName, String name, String tileName) {
        final Intent tileProbe = new Intent(TileService.ACTION_QS_TILE);
        final ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new FakeServiceInfo(packageName, name, tileName);
        final ShadowPackageManager shadowPackageManager =
                Shadows.shadowOf(mContext.getPackageManager());
        shadowPackageManager.addResolveInfoForIntent(tileProbe, info);
    }

    private AccessibilityServiceInfo setupServiceWarningRequired(boolean required)
            throws Throwable {
        final AccessibilityServiceInfo info = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
        mFragment.mComponentName = info.getComponentName();
        when(mContext.getSystemService(AccessibilityManager.class))
                .thenReturn(mMockAccessibilityManager);
        when(mMockAccessibilityManager.isAccessibilityServiceWarningRequired(any()))
                .thenReturn(required);
        when(mFragment.getAccessibilityServiceInfo()).thenReturn(info);
        return info;
    }

    private static class FakeServiceInfo extends ServiceInfo {
        private String mTileName;

        FakeServiceInfo(String packageName, String name, String tileName) {
            this.packageName = packageName;
            this.name = name;
            mTileName = tileName;
        }

        public String loadLabel(PackageManager mgr) {
            return mTileName;
        }
    }

    @NonNull
    private AccessibilityServiceInfo getFakeAccessibilityServiceInfo(String packageName,
            String className) throws Throwable {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = packageName;
        serviceInfo.packageName = packageName;
        serviceInfo.name = className;
        serviceInfo.applicationInfo = applicationInfo;
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;
        final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo, mContext);
        ComponentName componentName = ComponentName.createRelative(packageName, className);
        info.setComponentName(componentName);
        return info;
    }

    private static class TestToggleAccessibilityServicePreferenceFragment
            extends ToggleAccessibilityServicePreferenceFragment {
        int mLastShownDialogId = NO_DIALOG;

        @Override
        protected ComponentName getTileComponentName() {
            return PLACEHOLDER_TILE_COMPONENT_NAME;
        }

        @Override
        protected void showDialog(int dialogId) {
            mLastShownDialogId = dialogId;
        }
    }
}
