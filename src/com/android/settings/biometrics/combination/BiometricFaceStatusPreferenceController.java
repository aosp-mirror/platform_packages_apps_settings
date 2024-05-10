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
package com.android.settings.biometrics.combination;

import android.content.Context;

import androidx.lifecycle.Lifecycle;

import com.android.settings.Utils;
import com.android.settings.biometrics.face.FaceStatusPreferenceController;

/**
 * Preference controller for face settings within the biometrics settings page, that controls the
 * ability to unlock the phone with face authentication.
 */
public class BiometricFaceStatusPreferenceController extends FaceStatusPreferenceController {

    public BiometricFaceStatusPreferenceController(Context context, String key) {
        super(context, key, null /* lifecycle */);
    }

    public BiometricFaceStatusPreferenceController(
            Context context, String key, Lifecycle lifecycle) {
        super(context, key, lifecycle);
    }

    @Override
    protected boolean isDeviceSupported() {
        return Utils.isMultipleBiometricsSupported(mContext) && isHardwareSupported();
    }

    @Override
    protected boolean isHardwareSupported() {
        return Utils.hasFaceHardware(mContext);
    }
}
