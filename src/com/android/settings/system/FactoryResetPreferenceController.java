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
 * limitations under the License
 */
package com.android.settings.system;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

public class FactoryResetPreferenceController extends BasePreferenceController {

    private final UserManager mUm;

    public FactoryResetPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    /** Hide "Factory reset" settings for secondary users. */
    @Override
    public int getAvailabilityStatus() {
        return mUm.isAdminUser() ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (mPreferenceKey.equals(preference.getKey())) {
            final Intent intent = new Intent(mContext, Settings.FactoryResetActivity.class);
            mContext.startActivity(intent);
            return true;
        }
        return false;
    }
}
