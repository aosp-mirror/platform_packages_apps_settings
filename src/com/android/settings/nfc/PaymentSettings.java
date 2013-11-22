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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.android.settings.HelpUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;

import java.util.List;

public class PaymentSettings extends SettingsPreferenceFragment implements
        OnClickListener {
    public static final String TAG = "PaymentSettings";
    private LayoutInflater mInflater;
    private PaymentBackend mPaymentBackend;
    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPaymentBackend = new PaymentBackend(getActivity());
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setHasOptionsMenu(true);
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
                preference.setTitle(appInfo.caption);
                if (appInfo.banner != null) {
                    screen.addPreference(preference);
                } else {
                    // Ignore, no banner
                    Log.e(TAG, "Couldn't load banner drawable of service " + appInfo.componentName);
                }
            }
        }
        TextView emptyText = (TextView) getView().findViewById(R.id.nfc_payment_empty_text);
        TextView learnMore = (TextView) getView().findViewById(R.id.nfc_payment_learn_more);
        ImageView emptyImage = (ImageView) getView().findViewById(R.id.nfc_payment_tap_image);
        if (screen.getPreferenceCount() == 0) {
            emptyText.setVisibility(View.VISIBLE);
            learnMore.setVisibility(View.VISIBLE);
            emptyImage.setVisibility(View.VISIBLE);
            getListView().setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            learnMore.setVisibility(View.GONE);
            emptyImage.setVisibility(View.GONE);
            getListView().setVisibility(View.VISIBLE);
        }
        setPreferenceScreen(screen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = mInflater.inflate(R.layout.nfc_payment, container, false);
        TextView learnMore = (TextView) v.findViewById(R.id.nfc_payment_learn_more);
        learnMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String helpUrl;
                if (!TextUtils.isEmpty(helpUrl = getResources().getString(
                        R.string.help_url_nfc_payment))) {
                    final Uri fullUri = HelpUtils.uriWithAddedParameters(
                            PaymentSettings.this.getActivity(), Uri.parse(helpUrl));
                    Intent intent = new Intent(Intent.ACTION_VIEW, fullUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    startActivity(intent);
                } else {
                    Log.e(TAG, "Help url not set.");
                }
            }
        });
        return v;
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
        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        refresh();
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        String searchUri = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.PAYMENT_SERVICE_SEARCH_URI);
        if (!TextUtils.isEmpty(searchUri)) {
            MenuItem menuItem = menu.add(R.string.nfc_payment_menu_item_add_service);
            menuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menuItem.setIntent(new Intent(Intent.ACTION_VIEW,Uri.parse(searchUri)));
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            refresh();
        }
    };

    private class SettingsPackageMonitor extends PackageMonitor {
        @Override
        public void onPackageAdded(String packageName, int uid) {
           mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mHandler.obtainMessage().sendToTarget();
        }
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

            ImageView banner = (ImageView) view.findViewById(R.id.banner);
            banner.setImageDrawable(appInfo.banner);
        }
    }
}
