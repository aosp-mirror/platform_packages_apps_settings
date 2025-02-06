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

package com.android.settings.biometrics.face;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Feature provider for face unlock */
public interface FaceFeatureProvider {
    /** Returns specified intent config by resource R.string.config_face_enroll_guidance_page. */
    @Nullable
    Intent getPostureGuidanceIntent(Context context);

    /** Returns true if attention checking is supported. */
    boolean isAttentionSupported(Context context);

    /** Returns true if setup wizard supported face enrollment. */
    boolean isSetupWizardSupported(Context context);

    /**
     * Gets the provider for current face enrollment activity classes
     * @return the provider
     */
    @NonNull
    default FaceEnrollActivityClassProvider getEnrollActivityClassProvider() {
        return FaceEnrollActivityClassProvider.getInstance();
    }

    /**
     * Gets the feature provider for FaceSettings page
     * @return the provider
     */
    @NonNull
    default FaceSettingsFeatureProvider getFaceSettingsFeatureProvider() {
        return FaceSettingsFeatureProvider.getInstance();
    }
}
