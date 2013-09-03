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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;

import java.util.List;

public class PaymentSettings extends SettingsPreferenceFragment implements
        OnClickListener {
    public static final String TAG = "PaymentSettings";
    private PaymentBackend mPaymentBackend;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setHasOptionsMenu(false);
        mPaymentBackend = new PaymentBackend(getActivity());
    }

    public void refresh() {
        PreferenceManager manager = getPreferenceManager();
        PreferenceScreen screen = manager.createPreferenceScreen(getActivity());

        // Get all payment services
        List<PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();
        if (appInfos != null && appInfos.size() > 0) {
            // Add all payment apps
            for (PaymentAppInfo appInfo : appInfos) {
                PaymentAppPreference preference =
                        new PaymentAppPreference(getActivity(), appInfo, this);
                preference.setIcon(appInfo.icon);
                preference.setTitle(appInfo.caption);
                screen.addPreference(preference);
            }
        }
        setPreferenceScreen(screen);
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof PaymentAppInfo) {
            PaymentAppInfo appInfo = (PaymentAppInfo) v.getTag();
            if (appInfo.componentName != null) {
                mPaymentBackend.setDefaultPaymentApp(appInfo.componentName);
            }
            refresh();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public static class PaymentAppPreference extends Preference {
        private final OnClickListener listener;
        private final PaymentAppInfo appInfo;

        public PaymentAppPreference(Context context, PaymentAppInfo appInfo,
                OnClickListener listener) {
            super(context);
            setLayoutResource(R.layout.nfc_payment_option);
            this.appInfo = appInfo;
            this.listener = listener;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            view.setOnClickListener(listener);
            view.setTag(appInfo);

            RadioButton radioButton = (RadioButton) view.findViewById(android.R.id.button1);
            radioButton.setChecked(appInfo.isDefault);
        }
    }
}