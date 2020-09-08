/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.language;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.inputmethod.UserDictionaryListPreferenceController;
import com.android.settings.inputmethod.UserDictionarySettings;

import java.util.TreeSet;

public class UserDictionaryPreferenceController extends BasePreferenceController {

    public UserDictionaryPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (!isAvailable() || preference == null) {
            return;
        }
        final TreeSet<String> localeSet = getDictionaryLocales();
        final Bundle extras = preference.getExtras();
        final Class<? extends Fragment> targetFragment;
        if (localeSet.size() <= 1) {
            if (!localeSet.isEmpty()) {
                // If the size of localeList is 0, we don't set the locale
                // parameter in the extras. This will be interpreted by the
                // UserDictionarySettings class as meaning
                // "the current locale". Note that with the current code for
                // UserDictionaryListPreferenceController#getUserDictionaryLocalesSet()
                // the locale list always has at least one element, since it
                // always includes the current locale explicitly.
                // @see UserDictionaryListPreferenceController.getUserDictionaryLocalesSet().
                extras.putString("locale", localeSet.first());
            }
            targetFragment = UserDictionarySettings.class;
        } else {
            targetFragment = UserDictionaryList.class;
        }
        preference.setFragment(targetFragment.getCanonicalName());
    }

    protected TreeSet<String> getDictionaryLocales() {
        return UserDictionaryListPreferenceController.getUserDictionaryLocalesSet(mContext);
    }
}
