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

package com.android.settings.accessibility;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SurveyFeatureProvider;

/**
 * PreferenceController for magnification feedback preference. This controller manages the
 * visibility and click behavior of the preference based on the availability of a user survey
 * related to magnification.
 */
public class MagnificationFeedbackPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "MagnificationFeedbackPreferenceController";
    public static final String PREF_KEY = "magnification_feedback";
    public static final String FEEDBACK_KEY = "A11yMagnificationUser";
    private final DashboardFragment mParent;
    private final @Nullable SurveyFeatureProvider mSurveyFeatureProvider;

    public MagnificationFeedbackPreferenceController(@NonNull Context context,
            @NonNull DashboardFragment parent, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mParent = parent;
        mSurveyFeatureProvider =
                FeatureFactory.getFeatureFactory().getSurveyFeatureProvider(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        if (mSurveyFeatureProvider != null) {
            mSurveyFeatureProvider.checkSurveyAvailable(
                    mParent.getViewLifecycleOwner(),
                    FEEDBACK_KEY,
                    enabled -> {
                        final String summary = mContext.getString(enabled
                                ? R.string.accessibility_feedback_summary
                                : R.string.accessibility_feedback_disabled_summary);
                        preference.setSummary(summary);
                        preference.setEnabled(enabled);
                    });
        } else {
            Log.w(TAG, "SurveyFeatureProvider is not ready");
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (mSurveyFeatureProvider != null) {
            mSurveyFeatureProvider.sendActivityIfAvailable(FEEDBACK_KEY);
        }
        return true;
    }
}
