/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.aware;

import android.content.Context;

import androidx.fragment.app.Fragment;

public interface AwareFeatureProvider {
    /** Returns true if the aware sensor is supported. */
    boolean isSupported(Context context);

    /** Returns true if the aware feature is enabled. */
    boolean isEnabled(Context context);

    /** Show information dialog. */
    void showRestrictionDialog(Fragment parent);

    /** Return Quick Gestures Summary. */
    CharSequence getGestureSummary(Context context, boolean sensorSupported,
            boolean assistGestureEnabled, boolean assistGestureSilenceEnabled);
}
