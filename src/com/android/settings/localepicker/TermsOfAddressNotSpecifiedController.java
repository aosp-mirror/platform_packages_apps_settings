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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Configuration;

public class TermsOfAddressNotSpecifiedController extends TermsOfAddressBaseController {

    private static final String KEY_NOT_SPECIFIED = "key_terms_of_address_not_specified";

    public TermsOfAddressNotSpecifiedController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NOT_SPECIFIED;
    }

    @Override
    protected int getMetricsActionKey() {
        return SettingsEnums.ACTION_TERMS_OF_ADDRESS_NOT_SPECIFIED;
    }

    @Override
    protected int getGrammaticalGenderType() {
        return Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED;
    }
}
