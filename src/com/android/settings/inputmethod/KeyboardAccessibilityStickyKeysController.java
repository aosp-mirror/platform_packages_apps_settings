/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputSettings;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleObserver;

public class KeyboardAccessibilityStickyKeysController extends
        KeyboardAccessibilityController implements
        LifecycleObserver {
    public KeyboardAccessibilityStickyKeysController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return InputSettings.isAccessibilityStickyKeysEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setAccessibilityStickyKeysEnabled(mContext,
                isChecked);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return (super.getAvailabilityStatus() == AVAILABLE)
                && InputSettings.isAccessibilitySlowKeysFeatureFlagEnabled() ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    protected void updateKeyboardAccessibilitySettings() {
        setChecked(
                InputSettings.isAccessibilityStickyKeysEnabled(mContext));
    }

    @Override
    protected Uri getSettingUri() {
        return Settings.Secure.getUriFor(
                Settings.Secure.ACCESSIBILITY_STICKY_KEYS);
    }
}
