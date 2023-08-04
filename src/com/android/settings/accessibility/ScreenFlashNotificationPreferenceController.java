/*
 * Copyright (C) 2023 The Android Open Source Project
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
import static com.android.settings.accessibility.FlashNotificationsUtil.DEFAULT_SCREEN_FLASH_COLOR;

import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;

import java.util.function.Consumer;

/**
 * Controller for Screen flash notification.
 */
public class ScreenFlashNotificationPreferenceController extends TogglePreferenceController {

    private Fragment mParentFragment;
    private Preference mPreference;

    public ScreenFlashNotificationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    public void setParentFragment(Fragment parentFragment) {
        this.mParentFragment = parentFragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_FLASH_NOTIFICATION, OFF) != OFF;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().changed(
                getMetricsCategory(), getPreferenceKey(), isChecked ? 1 : 0);
        if (isChecked) {
            checkAndSetInitialColor();
        }
        return Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_FLASH_NOTIFICATION, (isChecked ? ON : OFF));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public CharSequence getSummary() {
        return FlashNotificationsUtil.getColorDescriptionText(mContext,
                Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR,
                        DEFAULT_SCREEN_FLASH_COLOR));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        refreshColorSummary();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey()) && mParentFragment != null) {

            final int initialColor = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR,
                    DEFAULT_SCREEN_FLASH_COLOR);

            final Consumer<Integer> consumer = color -> {
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR, color);
                refreshColorSummary();
            };

            ScreenFlashNotificationColorDialogFragment
                    .getInstance(initialColor, consumer)
                    .show(mParentFragment.getParentFragmentManager(),
                            ScreenFlashNotificationColorDialogFragment.class.getSimpleName());
            return true;
        }

        return super.handlePreferenceTreeClick(preference);
    }

    private void checkAndSetInitialColor() {
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR, Color.TRANSPARENT)
                == Color.TRANSPARENT) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR, DEFAULT_SCREEN_FLASH_COLOR);
        }
    }

    private void refreshColorSummary() {
        if (mPreference != null) mPreference.setSummary(getSummary());
    }
}
