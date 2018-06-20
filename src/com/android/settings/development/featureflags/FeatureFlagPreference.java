/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development.featureflags;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.util.FeatureFlagUtils;
import android.util.Log;

public class FeatureFlagPreference extends SwitchPreference {

    private final String mKey;
    private final boolean mIsPersistent;

    public FeatureFlagPreference(Context context, String key) {
        super(context);
        mKey = key;
        setKey(key);
        setTitle(key);
        mIsPersistent = FeatureFlagPersistent.isPersistent(key);
        boolean isFeatureEnabled;
        if (mIsPersistent) {
            isFeatureEnabled = FeatureFlagPersistent.isEnabled(context, key);
        } else {
            isFeatureEnabled = FeatureFlagUtils.isEnabled(context, key);
        }
        setCheckedInternal(isFeatureEnabled);
    }

    @Override
    public void setChecked(boolean isChecked) {
        setCheckedInternal(isChecked);
        if (mIsPersistent) {
            FeatureFlagPersistent.setEnabled(getContext(), mKey, isChecked);
        } else {
            FeatureFlagUtils.setEnabled(getContext(), mKey, isChecked);
        }
    }

    private void setCheckedInternal(boolean isChecked) {
        super.setChecked(isChecked);
        setSummary(Boolean.toString(isChecked));
    }
}
