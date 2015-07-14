/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;

import java.util.List;

public class NfcPaymentPreference extends DialogPreference implements
        DialogInterface.OnClickListener, PaymentBackend.Callback, View.OnClickListener {

    private static final String TAG = "NfcPaymentPreference";

    private final NfcPaymentAdapter mAdapter;
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final PaymentBackend mPaymentBackend;

    // Fields below only modified on UI thread
    private ImageView mSettingsButtonView;

    public NfcPaymentPreference(Context context, PaymentBackend backend) {
        super(context, null);
        mPaymentBackend = backend;
        mContext = context;
        backend.registerCallback(this);
        mAdapter = new NfcPaymentAdapter();
        setDialogTitle(context.getString(R.string.nfc_payment_pay_with));
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setWidgetLayoutResource(R.layout.preference_widget_settings);

        refresh();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mSettingsButtonView = (ImageView) view.findViewById(R.id.settings_button);
        mSettingsButtonView.setOnClickListener(this);

        updateSettingsVisibility();
    }

    /**
     * MUST be called on UI thread.
     */
    public void refresh() {
        List<PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();
        PaymentAppInfo defaultApp = mPaymentBackend.getDefaultApp();
        if (appInfos != null) {
            PaymentAppInfo[] apps = appInfos.toArray(new PaymentAppInfo[appInfos.size()]);
            mAdapter.updateApps(apps, defaultApp);
        }
        setTitle(R.string.nfc_payment_default);
        if (defaultApp != null) {
            setSummary(defaultApp.label);
        } else {
            setSummary(mContext.getString(R.string.nfc_payment_default_not_set));
        }
        updateSettingsVisibility();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setSingleChoiceItems(mAdapter, 0, this);
    }

    @Override
    public void onPaymentAppsChanged() {
        refresh();
    }

    @Override
    public void onClick(View view) {
        PaymentAppInfo defaultAppInfo = mPaymentBackend.getDefaultApp();
        if (defaultAppInfo != null && defaultAppInfo.settingsComponent != null) {
            Intent settingsIntent = new Intent(Intent.ACTION_MAIN);
            settingsIntent.setComponent(defaultAppInfo.settingsComponent);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mContext.startActivity(settingsIntent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Settings activity not found.");
            }
        }
    }

    void updateSettingsVisibility() {
        if (mSettingsButtonView != null) {
            PaymentAppInfo defaultApp = mPaymentBackend.getDefaultApp();
            if (defaultApp == null || defaultApp.settingsComponent == null) {
                mSettingsButtonView.setVisibility(View.GONE);
            } else {
                mSettingsButtonView.setVisibility(View.VISIBLE);

            }
        }
    }

    class NfcPaymentAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener,
            View.OnClickListener {
        // Only modified on UI thread
        private PaymentAppInfo[] appInfos;

        public NfcPaymentAdapter() {
        }

        public void updateApps(PaymentAppInfo[] appInfos, PaymentAppInfo currentDefault) {
            // Clone app infos, only add those with a banner
            this.appInfos = appInfos;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return appInfos.length;
        }

        @Override
        public PaymentAppInfo getItem(int i) {
            return appInfos[i];
        }

        @Override
        public long getItemId(int i) {
            return appInfos[i].componentName.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            PaymentAppInfo appInfo = appInfos[position];
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(
                        R.layout.nfc_payment_option, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.banner);
                holder.radioButton = (RadioButton) convertView.findViewById(R.id.button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.imageView.setImageDrawable(appInfo.banner);
            holder.imageView.setTag(appInfo);
            holder.imageView.setContentDescription(appInfo.label);
            holder.imageView.setOnClickListener(this);

            // Prevent checked callback getting called on recycled views
            holder.radioButton.setOnCheckedChangeListener(null);
            holder.radioButton.setChecked(appInfo.isDefault);
            holder.radioButton.setContentDescription(appInfo.label);
            holder.radioButton.setOnCheckedChangeListener(this);
            holder.radioButton.setTag(appInfo);
            return convertView;
        }

        public class ViewHolder {
            public ImageView imageView;
            public RadioButton radioButton;
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            PaymentAppInfo appInfo = (PaymentAppInfo) compoundButton.getTag();
            makeDefault(appInfo);
        }

        @Override
        public void onClick(View view) {
            PaymentAppInfo appInfo = (PaymentAppInfo) view.getTag();
            makeDefault(appInfo);
        }

        void makeDefault(PaymentAppInfo appInfo) {
            if (!appInfo.isDefault) {
                mPaymentBackend.setDefaultPaymentApp(appInfo.componentName);
            }
            getDialog().dismiss();
        }
    }
}
