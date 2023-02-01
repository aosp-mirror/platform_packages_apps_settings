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

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

/** PrimarySwitchPreferenceController that shows quick settings tooltip on first use. */
public abstract class AccessibilityQuickSettingsPrimarySwitchPreferenceController
        extends TogglePreferenceController
        implements LifecycleObserver, OnCreate, OnSaveInstanceState {
    private static final String KEY_SAVED_QS_TOOLTIP_RESHOW = "qs_tooltip_reshow";
    private final Handler mHandler;
    private PrimarySwitchPreference mPreference;
    private AccessibilityQuickSettingsTooltipWindow mTooltipWindow;
    private boolean mNeedsQSTooltipReshow = false;

    /** Returns the accessibility tile component name. */
    abstract ComponentName getTileComponentName();

    /** Returns the accessibility tile tooltip content. */
    abstract CharSequence getTileTooltipContent();

    public AccessibilityQuickSettingsPrimarySwitchPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mHandler = new Handler(context.getMainLooper());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Restore the tooltip.
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_SAVED_QS_TOOLTIP_RESHOW)) {
                mNeedsQSTooltipReshow = savedInstanceState.getBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mTooltipWindow != null) {
            outState.putBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW, mTooltipWindow.isShowing());
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mNeedsQSTooltipReshow) {
            mHandler.post(this::showQuickSettingsTooltipIfNeeded);
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            showQuickSettingsTooltipIfNeeded();
        }
        return isChecked;
    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    private void showQuickSettingsTooltipIfNeeded() {
        if (mPreference == null) {
            // Returns if no preference found by slice highlight menu.
            return;
        }

        final ComponentName tileComponentName = getTileComponentName();
        if (tileComponentName == null) {
            // Returns if no tile service assigned.
            return;
        }

        if (!mNeedsQSTooltipReshow && AccessibilityQuickSettingUtils.hasValueInSharedPreferences(
                mContext, tileComponentName)) {
            // Returns if quick settings tooltip only show once.
            return;
        }

        mTooltipWindow = new AccessibilityQuickSettingsTooltipWindow(mContext);
        mTooltipWindow.setup(getTileTooltipContent(),
                R.drawable.accessibility_auto_added_qs_tooltip_illustration);
        mTooltipWindow.showAtTopCenter(mPreference.getSwitch());
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(mContext, tileComponentName);
        mNeedsQSTooltipReshow = false;
    }
}
