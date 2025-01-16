/*
 * Copyright 2025 The Android Open Source Project
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

import static android.view.accessibility.AccessibilityManager.AUTOCLICK_IGNORE_MINOR_CURSOR_MOVEMENT_DEFAULT;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class ToggleAutoclickIgnoreMinorCursorMovementController extends TogglePreferenceController {

    private static final String TAG =
            ToggleAutoclickIgnoreMinorCursorMovementController.class.getSimpleName();

    private final ContentResolver mContentResolver;

    public ToggleAutoclickIgnoreMinorCursorMovementController(
            @NonNull Context context, @NonNull String key) {
        super(context, key);

        mContentResolver = context.getContentResolver();
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableAutoclickIndicator() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(
                        mContentResolver,
                        Settings.Secure.ACCESSIBILITY_AUTOCLICK_IGNORE_MINOR_CURSOR_MOVEMENT,
                        AUTOCLICK_IGNORE_MINOR_CURSOR_MOVEMENT_DEFAULT ? ON : OFF)
                == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_IGNORE_MINOR_CURSOR_MOVEMENT,
                isChecked ? ON : OFF);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
