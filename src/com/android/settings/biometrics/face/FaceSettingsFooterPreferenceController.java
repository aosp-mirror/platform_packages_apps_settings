/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.content.pm.PackageManager;
import android.hardware.biometrics.SensorProperties;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceAuthenticatorsRegisteredCallback;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.HelpUtils;

import java.util.List;

/**
 * Footer for face settings showing the help text and help link.
 */
public class FaceSettingsFooterPreferenceController extends BasePreferenceController {
    private static final String TAG = "FaceSettingsFooterPreferenceController";
    private static final String ANNOTATION_URL = "url";
    private final FaceFeatureProvider mProvider;
    private Preference mPreference;
    private boolean mIsFaceStrong;

    public FaceSettingsFooterPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mProvider = FeatureFactory.getFeatureFactory().getFaceFeatureProvider();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
        if (screen.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE)) {
            addAuthenticatorsRegisteredCallback(screen.getContext());
        } else {
            Log.w(TAG, "Not support FEATURE_FACE");
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        final Intent helpIntent = HelpUtils.getHelpIntent(
                mContext, mContext.getString(R.string.help_url_face), getClass().getName());
        final AnnotationSpan.LinkInfo linkInfo =
                new AnnotationSpan.LinkInfo(mContext, ANNOTATION_URL, helpIntent);

        int footerRes;
        boolean isAttentionSupported = mProvider.isAttentionSupported(mContext);
        if (mIsFaceStrong) {
            footerRes = isAttentionSupported
                    ? R.string.security_settings_face_settings_footer_class3
                    : R.string.security_settings_face_settings_footer_attention_not_supported;
        } else {
            footerRes = isAttentionSupported
                    ? R.string.security_settings_face_settings_footer
                    : R.string.security_settings_face_settings_footer_class3_attention_not_supported;
        }
        preference.setTitle(AnnotationSpan.linkify(
                mContext.getText(footerRes), linkInfo));
    }

    private void addAuthenticatorsRegisteredCallback(Context context) {
        final FaceManager faceManager = context.getSystemService(FaceManager.class);
        faceManager.addAuthenticatorsRegisteredCallback(
                new IFaceAuthenticatorsRegisteredCallback.Stub() {
                    @Override
                    public void onAllAuthenticatorsRegistered(
                            @NonNull List<FaceSensorPropertiesInternal> sensors) {
                        if (sensors.isEmpty()) {
                            Log.e(TAG, "No sensors");
                            return;
                        }

                        boolean isFaceStrong = sensors.get(0).sensorStrength
                                == SensorProperties.STRENGTH_STRONG;
                        if (mIsFaceStrong == isFaceStrong) {
                            return;
                        }
                        mIsFaceStrong = isFaceStrong;
                        updateState(mPreference);
                    }
                });
    }
}
