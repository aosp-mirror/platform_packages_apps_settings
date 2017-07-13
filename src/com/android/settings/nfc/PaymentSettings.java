/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.nfc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

public class PaymentSettings extends SettingsPreferenceFragment implements Indexable {
    public static final String TAG = "PaymentSettings";

    static final String PAYMENT_KEY = "payment";

    private PaymentBackend mPaymentBackend;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NFC_PAYMENT;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPaymentBackend = new PaymentBackend(getActivity());
        setHasOptionsMenu(true);

        PreferenceManager manager = getPreferenceManager();
        PreferenceScreen screen = manager.createPreferenceScreen(getActivity());

        List<PaymentBackend.PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();
        if (appInfos != null && appInfos.size() > 0) {
            NfcPaymentPreference preference =
                    new NfcPaymentPreference(getPrefContext(), mPaymentBackend);
            preference.setKey(PAYMENT_KEY);
            screen.addPreference(preference);
            NfcForegroundPreference foreground = new NfcForegroundPreference(getPrefContext(),
                    mPaymentBackend);
            screen.addPreference(foreground);
        }
        setPreferenceScreen(screen);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(
                R.layout.nfc_payment_empty, contentRoot, false);
        contentRoot.addView(emptyView);
        setEmptyView(emptyView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPaymentBackend.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaymentBackend.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem menuItem = menu.add(R.string.nfc_payment_how_it_works);
        Intent howItWorksIntent = new Intent(getActivity(), HowItWorks.class);
        menuItem.setIntent(howItWorksIntent);
        menuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.key = PAYMENT_KEY;
                data.title = res.getString(R.string.nfc_payment_settings_title);
                data.screenTitle = res.getString(R.string.nfc_payment_settings_title);
                data.keywords = res.getString(R.string.keywords_payment_settings);
                result.add(data);
                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> nonVisibleKeys = super.getNonIndexableKeys(context);
                final PackageManager pm = context.getPackageManager();
                if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                    return nonVisibleKeys;
                }
                nonVisibleKeys.add(PAYMENT_KEY);
                return nonVisibleKeys;
            }
        };
}
