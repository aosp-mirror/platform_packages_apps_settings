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
 * limitations under the License.
 */

package com.android.settings.support;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;

public class SupportPreferenceController extends BasePreferenceController {

    private final SupportFeatureProvider mSupportFeatureProvider;

    private Activity mActivity;

    public SupportPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSupportFeatureProvider = FeatureFactory.getFactory(context)
                .getSupportFeatureProvider(context);
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    @Override
    public int getAvailabilityStatus() {
        return mSupportFeatureProvider == null ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference == null || mActivity == null ||
                !TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        mSupportFeatureProvider.startSupport(mActivity);
        return true;

    }
}
