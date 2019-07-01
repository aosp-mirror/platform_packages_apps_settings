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
 * limitations under the License.
 */
package com.android.settings.privacy;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

public class WorkPolicyInfoPreferenceController extends BasePreferenceController {

    private final @NonNull EnterprisePrivacyFeatureProvider mEnterpriseProvider;

    public WorkPolicyInfoPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mEnterpriseProvider =
                FeatureFactory.getFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mEnterpriseProvider.hasWorkPolicyInfo()
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            mEnterpriseProvider.showWorkPolicyInfo();
            return true;
        }
        return false;
    }
}
