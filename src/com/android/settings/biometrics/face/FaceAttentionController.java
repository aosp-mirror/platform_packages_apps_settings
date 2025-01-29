/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import static android.hardware.biometrics.BiometricFaceConstants.FEATURE_REQUIRE_ATTENTION;

import android.content.Context;
import android.hardware.face.FaceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.Utils;

public class FaceAttentionController {

    @Nullable private byte[] mToken;
    private FaceManager mFaceManager;

    public interface OnSetAttentionListener {
        /**
         * Calls when setting attention is completed from FaceManager.
         */
        void onSetAttentionCompleted(boolean success);
    }

    public interface OnGetAttentionListener {
        /**
         * Calls when getting attention is completed from FaceManager.
         */
        void onGetAttentionCompleted(boolean success, boolean enabled);
    }

    @Nullable private OnSetAttentionListener mSetListener;
    @Nullable private OnGetAttentionListener mGetListener;

    private final FaceManager.SetFeatureCallback mSetFeatureCallback =
            new FaceManager.SetFeatureCallback() {
                @Override
                public void onCompleted(boolean success, int feature) {
                    if (feature == FEATURE_REQUIRE_ATTENTION) {
                        if (mSetListener != null) {
                            mSetListener.onSetAttentionCompleted(success);
                        }
                    }
                }
            };

    private final FaceManager.GetFeatureCallback mGetFeatureCallback =
            new FaceManager.GetFeatureCallback() {
                @Override
                public void onCompleted(
                        boolean success, @NonNull int[] features, @NonNull boolean[] featureState) {
                    boolean requireAttentionEnabled = false;
                    for (int i = 0; i < features.length; i++) {
                        if (features[i] == FEATURE_REQUIRE_ATTENTION) {
                            requireAttentionEnabled = featureState[i];
                        }
                    }
                    if (mGetListener != null) {
                        mGetListener.onGetAttentionCompleted(success, requireAttentionEnabled);
                    }
                }
            };

    public FaceAttentionController(@NonNull Context context) {
        mFaceManager = Utils.getFaceManagerOrNull(context);
    }

    /**
     * Set the challenge token
     */
    public void setToken(@Nullable byte[] token) {
        mToken = token;
    }

    /**
     * Get the gaze status
     */
    public void getAttentionStatus(int userId,
            @Nullable OnGetAttentionListener listener) {
        mGetListener = listener;
        mFaceManager.getFeature(userId, FEATURE_REQUIRE_ATTENTION, mGetFeatureCallback);
    }

    /**
     * Set the gaze status
     */
    public void setAttentionStatus(
            int userId, boolean enabled, @Nullable OnSetAttentionListener listener) {
        mSetListener = listener;
        mFaceManager.setFeature(userId, FEATURE_REQUIRE_ATTENTION, enabled, mToken,
                mSetFeatureCallback);
    }
}
