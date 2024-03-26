/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.accessibility.shortcuts;

import static com.android.internal.accessibility.common.ShortcutConstants.AccessibilityFragmentType.INVISIBLE_TOGGLE;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.service.quicksettings.TileService;
import android.util.ArraySet;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.Flags;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.util.AccessibilityUtils;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;
import com.android.settingslib.utils.StringUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A controller handles displaying the quick settings shortcut option preference and
 * configuring the shortcut.
 */
public class QuickSettingsShortcutOptionController extends ShortcutOptionPreferenceController {
    public QuickSettingsShortcutOptionController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @ShortcutConstants.UserShortcutType
    @Override
    protected int getShortcutType() {
        return ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof ShortcutOptionPreference shortcutOptionPreference) {
            shortcutOptionPreference.setTitle(
                    R.string.accessibility_shortcut_edit_dialog_title_quick_settings);
            shortcutOptionPreference.setIntroImageResId(
                    R.drawable.a11y_shortcut_type_quick_settings);
        }
    }

    @Override
    public CharSequence getSummary() {
        int numFingers = AccessibilityUtil.isTouchExploreEnabled(mContext) ? 2 : 1;
        return StringUtil.getIcuPluralsString(
                mContext,
                numFingers,
                isInSetupWizard()
                        ? R.string.accessibility_shortcut_edit_dialog_summary_quick_settings_suw
                        : R.string.accessibility_shortcut_edit_dialog_summary_quick_settings);
    }

    @Override
    protected boolean isShortcutAvailable() {
        return Flags.a11yQsShortcut()
                && TileService.isQuickSettingsSupported()
                && allTargetsHasQsTile()
                && allTargetsHasValidQsTileUseCase();
    }

    private boolean allTargetsHasQsTile() {
        AccessibilityManager accessibilityManager = mContext.getSystemService(
                AccessibilityManager.class);
        if (accessibilityManager == null) {
            return false;
        }

        Map<ComponentName, ComponentName> a11yFeatureToTileMap =
                accessibilityManager.getA11yFeatureToTileMap(UserHandle.myUserId());
        if (a11yFeatureToTileMap.isEmpty()) {
            return false;
        }
        for (String target : getShortcutTargets()) {
            ComponentName targetComponentName = ComponentName.unflattenFromString(target);
            if (targetComponentName == null
                    || !a11yFeatureToTileMap.containsKey(targetComponentName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if all targets have valid QS Tile shortcut use case.
     *
     * <p>
     * Note: We don't want to promote the qs option in the edit shortcuts screen for
     * a standard AccessibilityService, because the Tile is provided by the owner of the
     * AccessibilityService, and they don't have control to enable the A11yService themselves
     * which makes the TileService not acting as the other a11y shortcut like FAB where the user
     * can turn on/off the feature by toggling the shortcut.
     *
     * A standard AccessibilityService normally won't create a TileService because the
     * above mentioned reason. In any case where the standard AccessibilityService provides a tile,
     * we'll hide it from the Setting's UI.
     * </p>
     */
    private boolean allTargetsHasValidQsTileUseCase() {
        AccessibilityManager accessibilityManager = mContext.getSystemService(
                AccessibilityManager.class);
        if (accessibilityManager == null) {
            return false;
        }

        List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        final Set<String> standardA11yServices = new ArraySet<>();
        for (AccessibilityServiceInfo serviceInfo : installedServices) {
            if (AccessibilityUtils.getAccessibilityServiceFragmentType(serviceInfo)
                    != INVISIBLE_TOGGLE) {
                standardA11yServices.add(serviceInfo.getComponentName().flattenToString());
            }
        }

        for (String target : getShortcutTargets()) {
            if (standardA11yServices.contains(target)) {
                return false;
            }
        }

        return true;
    }
}
