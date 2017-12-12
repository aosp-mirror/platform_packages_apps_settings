/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.applications;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settingslib.core.AbstractPreferenceController;

public class SpecialAppAccessPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_SPECIAL_ACCESS = "special_access";

    private DataSaverBackend mDataSaverBackend;

    public SpecialAppAccessPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SPECIAL_ACCESS;
    }

    @Override
    public void updateState(Preference preference) {
        if (mDataSaverBackend == null) {
            mDataSaverBackend = new DataSaverBackend(mContext);
        }
        final int count = mDataSaverBackend.getWhitelistedCount();
        preference.setSummary(mContext.getResources().getQuantityString(
            R.plurals.special_access_summary, count, count));
    }
}
