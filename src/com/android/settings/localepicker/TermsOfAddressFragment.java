/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class TermsOfAddressFragment extends DashboardFragment {

    private static final String LOG_TAG = "TermsOfAddressFragment";
    private static final String KEY_NOT_SPECIFIED = "key_terms_of_address_not_specified";
    private static final String KEY_FEMININE = "key_terms_of_address_feminine";
    private static final String KEY_MASCULINE = "key_terms_of_address_masculine";
    private static final String KEY_NEUTRAL = "key_terms_of_address_neutral";

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TERMS_OF_ADDRESS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.terms_of_address;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new TermsOfAddressNotSpecifiedController(context, KEY_NOT_SPECIFIED));
        controllers.add(new TermsOfAddressFeminineController(context, KEY_FEMININE));
        controllers.add(new TermsOfAddressMasculineController(context, KEY_MASCULINE));
        controllers.add(new TermsOfAddressNeutralController(context, KEY_NEUTRAL));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.terms_of_address);
}
