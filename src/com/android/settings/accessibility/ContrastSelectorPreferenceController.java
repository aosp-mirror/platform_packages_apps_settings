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

package com.android.settings.accessibility;

import static android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_HIGH;
import static android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_MEDIUM;
import static android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_STANDARD;
import static android.app.UiModeManager.ContrastUtils.fromContrastLevel;
import static android.app.UiModeManager.ContrastUtils.toContrastLevel;

import android.app.UiModeManager;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.LayoutPreference;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Controller for contrast selector.
 */
public class ContrastSelectorPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, UiModeManager.ContrastChangeListener {

    private static final String KEY_COLOR_CONTRAST_SELECTOR = "color_contrast_selector";

    private final Executor mMainExecutor;
    private final UiModeManager mUiModeManager;
    private Map<Integer, FrameLayout> mContrastButtons = new HashMap<>();

    public ContrastSelectorPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);

        mMainExecutor = mContext.getMainExecutor();
        mUiModeManager = mContext.getSystemService(UiModeManager.class);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);

        final LayoutPreference mLayoutPreference =
                screen.findPreference(KEY_COLOR_CONTRAST_SELECTOR);

        mContrastButtons = Map.ofEntries(
                Map.entry(CONTRAST_LEVEL_STANDARD,
                        mLayoutPreference.findViewById(R.id.contrast_button_default)),
                Map.entry(CONTRAST_LEVEL_MEDIUM,
                        mLayoutPreference.findViewById(R.id.contrast_button_medium)),
                Map.entry(CONTRAST_LEVEL_HIGH,
                        mLayoutPreference.findViewById(R.id.contrast_button_high))
        );

        mContrastButtons.forEach((contrastLevel, contrastButton) -> {
            contrastButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(@Nullable View v) {
                    Settings.Secure.putFloat(mContext.getContentResolver(),
                            Settings.Secure.CONTRAST_LEVEL,
                            fromContrastLevel(contrastLevel));
                }
            });
        });

        highlightContrast(toContrastLevel(mUiModeManager.getContrast()));
    }

    @Override
    public int getAvailabilityStatus() {
        // The main preferences screen is feature guarded, so this always returns AVAILABLE.
        return AVAILABLE;
    }

    @Override
    public void onStart() {
        mUiModeManager.addContrastChangeListener(mMainExecutor, this);
    }

    @Override
    public void onStop() {
        mUiModeManager.removeContrastChangeListener(this);
    }

    @Override
    public void onContrastChanged(float contrast) {
        highlightContrast(toContrastLevel(contrast));
    }

    private void highlightContrast(int contrast) {
        mContrastButtons.forEach((level, button) -> {
            button.setSelected(level == contrast);
        });
    }
}
