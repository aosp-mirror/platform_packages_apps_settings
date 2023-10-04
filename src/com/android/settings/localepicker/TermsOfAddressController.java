/**
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.localepicker;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TermsOfAddressController extends BasePreferenceController {

    private static final String TAG = "TermsOfAddressController";
    private static final String KEY = "key_terms_of_address";

    private Preference mPreference;

    public TermsOfAddressController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setFragment(TermsOfAddressFragment.class.getCanonicalName());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
