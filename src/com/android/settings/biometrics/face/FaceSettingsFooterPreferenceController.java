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
import android.content.res.ResourceId;
import android.hardware.biometrics.SensorProperties;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceAuthenticatorsRegisteredCallback;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

import java.util.List;

/**
 * Footer for face settings showing the help text and help link.
 */
public class FaceSettingsFooterPreferenceController extends BasePreferenceController {
    private static final String KEY = "security_face_footer";
    private static final String TAG = "FaceSettingsFooterPreferenceController";
    private static final String ANNOTATION_URL = "url";
    private final FaceFeatureProvider mProvider;
    private Preference mPreference;
    private boolean mIsFaceStrong;
    private int mUserId;

    public FaceSettingsFooterPreferenceController(@NonNull Context context) {
        this(context, KEY);
    }
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
        final FaceSettingsFeatureProvider featureProvider = FeatureFactory.getFeatureFactory()
                .getFaceFeatureProvider().getFaceSettingsFeatureProvider();
        final int footerRes;
        boolean isAttentionSupported = mProvider.isAttentionSupported(mContext);
        if (Utils.isPrivateProfile(mUserId, mContext)) {
            footerRes = R.string.private_space_face_settings_footer;
        } else if (mIsFaceStrong) {
            footerRes = isAttentionSupported
                    ? featureProvider.getSettingPageFooterDescriptionClass3()
                    : R.string.security_settings_face_settings_footer_attention_not_supported;
        } else {
            footerRes = isAttentionSupported
                    ? R.string.security_settings_face_settings_footer
                    : R.string.security_settings_face_settings_footer_class3_attention_not_supported;
        }
        preference.setTitle(AnnotationSpan.linkify(
                mContext.getText(footerRes), linkInfo));

        final int learnMoreRes = featureProvider.getSettingPageFooterLearnMoreDescription();
        final int learnMoreUrlRes = featureProvider.getSettingPageFooterLearnMoreUrl();
        if (ResourceId.isValid(learnMoreRes)
                && ResourceId.isValid(learnMoreUrlRes)
                && preference instanceof FooterPreference) {
            final Intent learnMoreIntent = HelpUtils.getHelpIntent(
                    mContext, mContext.getString(learnMoreUrlRes), getClass().getName());
            final View.OnClickListener learnMoreClickListener = (v) -> {
                mContext.startActivityForResult(KEY, learnMoreIntent, 0, null);
            };
            ((FooterPreference) preference).setLearnMoreAction(learnMoreClickListener);
            ((FooterPreference) preference).setLearnMoreText(mContext.getString(learnMoreRes));
        }
    }

    public void setUserId(int userId) {
        mUserId = userId;
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
