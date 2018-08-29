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

package com.android.settings.development;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

public class HdcpCheckingPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String HDCP_CHECKING_KEY = "hdcp_checking";

    @VisibleForTesting
    static final String HDCP_CHECKING_PROPERTY = "persist.sys.hdcp_checking";
    @VisibleForTesting
    static final String USER_BUILD_TYPE = "user";

    private final String[] mListValues;
    private final String[] mListSummaries;

    public HdcpCheckingPreferenceController(Context context) {
        super(context);

        mListValues = mContext.getResources().getStringArray(R.array.hdcp_checking_values);
        mListSummaries = mContext.getResources().getStringArray(R.array.hdcp_checking_summaries);
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.equals(USER_BUILD_TYPE, getBuildType());
    }

    @Override
    public String getPreferenceKey() {
        return HDCP_CHECKING_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SystemProperties.set(HDCP_CHECKING_PROPERTY, newValue.toString());
        updateHdcpValues((ListPreference) mPreference);
        SystemPropPoker.getInstance().poke();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateHdcpValues((ListPreference) mPreference);
    }

    private void updateHdcpValues(ListPreference preference) {
        final String currentValue = SystemProperties.get(HDCP_CHECKING_PROPERTY);
        int index = 1; // Defaults to drm-only. Needs to match with R.array.hdcp_checking_values
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }
        preference.setValue(mListValues[index]);
        preference.setSummary(mListSummaries[index]);
    }

    @VisibleForTesting
    public String getBuildType() {
        return Build.TYPE;
    }
}
