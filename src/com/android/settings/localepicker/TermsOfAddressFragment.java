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

public class TermsOfAddressFragment extends DashboardFragment {

    private static final String LOG_TAG = "TermsOfAddressFragment";

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
    public void onAttach(Context context) {
        super.onAttach(context);
        TermsOfAddressHelper termsOfAddressHelper = new TermsOfAddressHelper(context);
        use(TermsOfAddressNotSpecifiedController.class).setTermsOfAddressHelper(
                termsOfAddressHelper);
        use(TermsOfAddressFeminineController.class).setTermsOfAddressHelper(termsOfAddressHelper);
        use(TermsOfAddressMasculineController.class).setTermsOfAddressHelper(termsOfAddressHelper);
        use(TermsOfAddressNeutralController.class).setTermsOfAddressHelper(termsOfAddressHelper);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.terms_of_address);
}
