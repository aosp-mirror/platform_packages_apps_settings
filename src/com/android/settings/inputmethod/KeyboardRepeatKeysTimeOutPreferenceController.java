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

import androidx.annotation.NonNull;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.SliderPreferenceController;

import com.google.common.collect.ImmutableList;

public class KeyboardRepeatKeysTimeOutPreferenceController extends SliderPreferenceController {
    @VisibleForTesting
    static final ImmutableList<Integer> REPEAT_KEY_TIMEOUT_VALUE_LIST = ImmutableList.of(2000, 1500,
            1000, 400, 300, 200, 150);

    public KeyboardRepeatKeysTimeOutPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getSliderPosition() {
        return REPEAT_KEY_TIMEOUT_VALUE_LIST.indexOf(InputSettings.getRepeatKeysTimeout(mContext));
    }

    @Override
    public boolean setSliderPosition(int position) {
        InputSettings.setRepeatKeysTimeout(mContext, REPEAT_KEY_TIMEOUT_VALUE_LIST.get(position));
        return true;
    }

    @Override
    public int getMax() {
        return REPEAT_KEY_TIMEOUT_VALUE_LIST.size() - 1;
    }

    @Override
    public int getMin() {
        return 0;
    }

    @Override
    public int getAvailabilityStatus() {
        return InputSettings.isRepeatKeysFeatureFlagEnabled()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
