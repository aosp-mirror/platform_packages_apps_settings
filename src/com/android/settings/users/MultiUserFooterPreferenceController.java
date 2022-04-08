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

package com.android.settings.users;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class MultiUserFooterPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    final UserCapabilities mUserCaps;

    public MultiUserFooterPreferenceController(Context context, String key) {
        super(context, key);
        mUserCaps = UserCapabilities.create(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return (mUserCaps.mEnabled && !mUserCaps.mUserSwitcherEnabled)
                ? AVAILABLE_UNSEARCHABLE
                : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        mUserCaps.updateAddUserCapabilities(mContext);
        preference.setVisible(isAvailable());
    }
}
