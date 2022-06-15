/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.nfc;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

public class NfcPaymentPreferenceController extends BasePreferenceController implements
        PaymentBackend.Callback, View.OnClickListener, NfcPaymentPreference.Listener,
        LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "NfcPaymentController";

    private final NfcPaymentAdapter mAdapter;
    private PaymentBackend mPaymentBackend;
    private NfcPaymentPreference mPreference;
    private ImageView mSettingsButtonView;

    public NfcPaymentPreferenceController(Context context, String key) {
        super(context, key);
        mAdapter = new NfcPaymentAdapter(context);
    }

    public void setPaymentBackend(PaymentBackend backend) {
        mPaymentBackend = backend;
    }

    @Override
    public void onStart() {
        if (mPaymentBackend != null) {
            mPaymentBackend.registerCallback(this);
        }
    }

    @Override
    public void onStop() {
        if (mPaymentBackend != null) {
            mPaymentBackend.unregisterCallback(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        final PackageManager pm = mContext.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (NfcAdapter.getDefaultAdapter(mContext) == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mPaymentBackend == null) {
            mPaymentBackend = new PaymentBackend(mContext);
        }
        final List<PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();
        return (appInfos != null && !appInfos.isEmpty())
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.initialize(this);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        mSettingsButtonView = (ImageView) view.findViewById(R.id.settings_button);
        mSettingsButtonView.setOnClickListener(this);

        updateSettingsVisibility();
    }

    @Override
    public void updateState(Preference preference) {
        final List<PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();
        if (appInfos != null) {
            final PaymentAppInfo[] apps = appInfos.toArray(new PaymentAppInfo[appInfos.size()]);
            mAdapter.updateApps(apps);
        }
        super.updateState(preference);
        updateSettingsVisibility();
    }

    @Override
    public CharSequence getSummary() {
        final PaymentAppInfo defaultApp = mPaymentBackend.getDefaultApp();
        if (defaultApp != null) {
            return defaultApp.label;
        } else {
            return mContext.getText(R.string.nfc_payment_default_not_set);
        }
    }

    @Override
    public void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        builder.setSingleChoiceItems(mAdapter, 0, listener);
    }

    @Override
    public void onPaymentAppsChanged() {
        updateState(mPreference);
    }

    @Override
    public void onClick(View view) {
        final PaymentAppInfo defaultAppInfo = mPaymentBackend.getDefaultApp();
        if (defaultAppInfo != null && defaultAppInfo.settingsComponent != null) {
            final Intent settingsIntent = new Intent(Intent.ACTION_MAIN);
            settingsIntent.setComponent(defaultAppInfo.settingsComponent);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mContext.startActivity(settingsIntent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Settings activity not found.");
            }
        }
    }

    private void updateSettingsVisibility() {
        if (mSettingsButtonView != null) {
            final PaymentAppInfo defaultApp = mPaymentBackend.getDefaultApp();
            if (defaultApp == null || defaultApp.settingsComponent == null) {
                mSettingsButtonView.setVisibility(View.GONE);
            } else {
                mSettingsButtonView.setVisibility(View.VISIBLE);
            }
        }
    }

    private class NfcPaymentAdapter extends BaseAdapter implements
            CompoundButton.OnCheckedChangeListener, View.OnClickListener {
        private final LayoutInflater mLayoutInflater;

        // Only modified on UI thread
        private PaymentAppInfo[] appInfos;

        public NfcPaymentAdapter(Context context) {
            mLayoutInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        public void updateApps(PaymentAppInfo[] appInfos) {
            // Clone app infos, only add an application label
            this.appInfos = appInfos;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return (appInfos != null) ? appInfos.length : 0;
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
            final PaymentAppInfo appInfo = appInfos[position];
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(
                        R.layout.nfc_payment_option, parent, false);
                holder = new ViewHolder();
                holder.radioButton = convertView.findViewById(R.id.button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // Prevent checked callback getting called on recycled views
            holder.radioButton.setOnCheckedChangeListener(null);
            holder.radioButton.setChecked(appInfo.isDefault);
            holder.radioButton.setContentDescription(appInfo.label);
            holder.radioButton.setOnCheckedChangeListener(this);
            holder.radioButton.setTag(appInfo);
            holder.radioButton.setText(appInfo.label);
            return convertView;
        }

        private class ViewHolder {
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

        private void makeDefault(PaymentAppInfo appInfo) {
            if (!appInfo.isDefault) {
                mPaymentBackend.setDefaultPaymentApp(appInfo.componentName);
            }
            final Dialog dialog = mPreference.getDialog();
            if (dialog != null) {
                dialog.dismiss();
            }
        }
    }
}
