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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.hidden_from_bootclasspath.android.permission.flags.Flags;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class PaymentSettings extends DashboardFragment {
    public static final String TAG = "PaymentSettings";

    private PaymentBackend mPaymentBackend;

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NFC_PAYMENT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.nfc_payment_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new NfcDefaultPaymentPreferenceController(context, lifecycle));

        return controllers;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPaymentBackend = new PaymentBackend(getActivity());

        use(NfcForegroundPreferenceController.class).setPaymentBackend(mPaymentBackend);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isShowEmptyImage(getPreferenceScreen())) {
            View emptyView = getActivity().getLayoutInflater().inflate(
                    R.layout.nfc_payment_empty, null, false);
            ((ViewGroup) view.findViewById(android.R.id.list_container)).addView(emptyView);
        }
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

    @VisibleForTesting
    boolean isShowEmptyImage(PreferenceScreen screen) {
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            final Preference preference = screen.getPreference(i);
            if(preference.isVisible()) {
                return false;
            }
        }
        return true;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.nfc_payment_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    if (Flags.walletRoleEnabled()) {
                        return false;
                    }
                    final UserManager userManager = context.getSystemService(UserManager.class);
                    final UserInfo myUserInfo = userManager.getUserInfo(UserHandle.myUserId());
                    if (myUserInfo.isGuest()) {
                        return false;
                    }
                    final PackageManager pm = context.getPackageManager();
                    return pm.hasSystemFeature(PackageManager.FEATURE_NFC);
                }
            };
}
