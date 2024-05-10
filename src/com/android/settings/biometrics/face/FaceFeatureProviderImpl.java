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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

public class FaceFeatureProviderImpl implements FaceFeatureProvider {

    /**
     * Returns the guidance page intent if device support {@link FoldingFeature}, and we want to
     * guide user enrolling faces with specific device posture.
     *
     * @param context the application context
     * @return the posture guidance intent, otherwise null if device not support
     */
    @Nullable
    @Override
    public Intent getPostureGuidanceIntent(Context context) {
        final String flattenedString = context.getString(R.string.config_face_enroll_guidance_page);
        final Intent intent;
        if (!TextUtils.isEmpty(flattenedString)) {
            ComponentName componentName = ComponentName.unflattenFromString(flattenedString);
            if (componentName != null) {
                intent = new Intent();
                intent.setComponent(componentName);
                return intent;
            }
        }
        return null;
    }

    @Override
    public boolean isAttentionSupported(Context context) {
        return true;
    }

    @Override
    public boolean isSetupWizardSupported(@NonNull Context context) {
        return true;
    }
}
