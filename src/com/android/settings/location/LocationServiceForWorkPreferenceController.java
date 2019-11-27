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

package com.android.settings.location;

import android.content.Context;
import android.os.UserHandle;

import androidx.preference.Preference;

import com.android.settings.widget.RestrictedAppPreference;

import java.util.List;
import java.util.Map;

/**
 * Retrieve the Location Services used in profile user.
 */
public class LocationServiceForWorkPreferenceController extends
        LocationServicePreferenceController {
    private static final String TAG = "LocationWorkPrefCtrl";

    public LocationServiceForWorkPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState(Preference preference) {
        mCategoryLocationServices.removeAll();
        final Map<Integer, List<Preference>> prefs = getLocationServices();
        boolean show = false;
        for (Map.Entry<Integer, List<Preference>> entry : prefs.entrySet()) {
            for (Preference pref : entry.getValue()) {
                if (pref instanceof RestrictedAppPreference) {
                    ((RestrictedAppPreference) pref).checkRestrictionAndSetDisabled();
                }
            }
            if (entry.getKey() != UserHandle.myUserId()) {
                LocationSettings.addPreferencesSorted(entry.getValue(),
                        mCategoryLocationServices);
                show = true;
            }
        }
        mCategoryLocationServices.setVisible(show);
    }
}
