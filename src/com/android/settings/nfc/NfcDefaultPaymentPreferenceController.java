/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

/**
 * NfcDefaultPaymentPreferenceController shows an app icon and text summary for current selected
 * default payment, and links to the nfc default payment selection page.
 */
public class NfcDefaultPaymentPreferenceController extends DefaultAppPreferenceController implements
        PaymentBackend.Callback, LifecycleObserver, OnResume, OnPause {

    private static final String TAG = "NfcDefaultPaymentController";
    private static final String KEY = "nfc_payment_app";

    private PaymentBackend mPaymentBackend;
    private Preference mPreference;
    private Context mContext;

    public NfcDefaultPaymentPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mContext = context;
        mPaymentBackend = new PaymentBackend(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        final PackageManager pm = mContext.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            return false;
        }
        if (NfcAdapter.getDefaultAdapter(mContext) == null) {
            return false;
        }
        if (mPaymentBackend == null) {
            mPaymentBackend = new PaymentBackend(mContext);
        }
        final List<PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();
        return (appInfos != null && !appInfos.isEmpty())
                ? true
                : false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onResume() {
        if (mPaymentBackend != null) {
            mPaymentBackend.registerCallback(this);
            mPaymentBackend.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mPaymentBackend != null) {
            mPaymentBackend.unregisterCallback(this);
            mPaymentBackend.onPause();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreference = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setIconSpaceReserved(true);
    }

    @Override
    public void onPaymentAppsChanged() {
        updateState(mPreference);
    }

    /**
     * PaymentDefaultAppInfo is used to store the default payment app info.
     */
    public static class PaymentDefaultAppInfo extends DefaultAppInfo {
        public PaymentAppInfo mInfo;

        public PaymentDefaultAppInfo(Context context, PackageManager pm, int userId,
                PaymentAppInfo info) {
            super(context, pm, userId, info.componentName);
            mInfo = info;
        }

        @Override
        public Drawable loadIcon() {
            return mInfo.icon;
        }
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        if (mPaymentBackend == null) {
            return null;
        }
        final PaymentAppInfo defaultApp = mPaymentBackend.getDefaultApp();
        if (defaultApp != null) {
            return new PaymentDefaultAppInfo(mContext, mPackageManager,
                    defaultApp.userHandle.getIdentifier(), defaultApp);
        }
        return null;
    }
}
