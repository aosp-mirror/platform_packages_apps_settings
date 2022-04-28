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

import static com.android.internal.accessibility.AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_TILE_SERVICE_COMPONENT_NAME;

import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/** PreferenceController that shows the Reduce Bright Colors summary */
public class ReduceBrightColorsPreferenceController
        extends AccessibilityQuickSettingsPrimarySwitchPreferenceController
        implements LifecycleObserver, OnStart, OnStop {
    private ContentObserver mSettingsContentObserver;
    private PrimarySwitchPreference mPreference;
    private final Context mContext;
    private final ColorDisplayManager mColorDisplayManager;

    public ReduceBrightColorsPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mSettingsContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())){
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                final String path = uri == null ? null : uri.getLastPathSegment();
                if (TextUtils.equals(path, Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED)) {
                    updateState(mPreference);
                }
            }
        };
        mColorDisplayManager = mContext.getSystemService(ColorDisplayManager.class);
    }

    @Override
    public boolean isChecked() {
        return mColorDisplayManager.isReduceBrightColorsActivated();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        super.setChecked(isChecked);
        return mColorDisplayManager.setReduceBrightColorsActivated(isChecked);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(
                R.string.reduce_bright_colors_preference_summary);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isReduceBrightColorsAvailable(mContext) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED),
                false, mSettingsContentObserver, UserHandle.USER_CURRENT);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }

    @Override
    protected ComponentName getTileComponentName() {
        return REDUCE_BRIGHT_COLORS_TILE_SERVICE_COMPONENT_NAME;
    }

    @Override
    CharSequence getTileTooltipContent() {
        return mContext.getText(
                R.string.accessibility_reduce_bright_colors_auto_added_qs_tooltip_content);
    }
}
