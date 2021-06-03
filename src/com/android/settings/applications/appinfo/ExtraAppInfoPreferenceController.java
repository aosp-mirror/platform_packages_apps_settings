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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;

/** Contains logic that deals with showing extra app info in app settings. */
public class ExtraAppInfoPreferenceController extends BasePreferenceController {

    private final ExtraAppInfoFeatureProvider mExtraAppInfoFeatureProvider;

    public ExtraAppInfoPreferenceController(Context context, String key) {
        super(context, key);
        mExtraAppInfoFeatureProvider =
                FeatureFactory.getFactory(context).getExtraAppInfoFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return mExtraAppInfoFeatureProvider.isSupported(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            mExtraAppInfoFeatureProvider.launchExtraAppInfoSettings(mContext);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    @Override
    public CharSequence getSummary() {
        return mExtraAppInfoFeatureProvider.getSummary(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        if (mExtraAppInfoFeatureProvider != null) {
            final Preference preference = screen.findPreference(getPreferenceKey());
            preference.setEnabled(mExtraAppInfoFeatureProvider.isEnabled(preference.getContext()));
        }
    }

    /**
     * Set the local package name
     */
    public void setPackageName(String packageName) {
        if (mExtraAppInfoFeatureProvider != null) {
            mExtraAppInfoFeatureProvider.setPackageName(packageName);
        }
    }
}
