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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulationManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

public class PaymentBackend {
    public static final String TAG = "Settings.PaymentBackend";

    public static class PaymentAppInfo {
        CharSequence caption;
        Drawable icon;
        boolean isDefault;
        public ComponentName componentName;
    }

    private final Context mContext;
    private final NfcAdapter mAdapter;
    private final CardEmulationManager mCardEmuManager;

    public PaymentBackend(Context context) {
        mContext = context;

        mAdapter = NfcAdapter.getDefaultAdapter(context);
        mCardEmuManager = CardEmulationManager.getInstance(mAdapter);
    }

    public List<PaymentAppInfo> getPaymentAppInfos() {
        PackageManager pm = mContext.getPackageManager();
        List<ApduServiceInfo> serviceInfos =
                mCardEmuManager.getServices(CardEmulationManager.CATEGORY_PAYMENT);
        List<PaymentAppInfo> appInfos = new ArrayList<PaymentAppInfo>();

        if (serviceInfos == null) return appInfos;

        ComponentName defaultApp = getDefaultPaymentApp();

        for (ApduServiceInfo service : serviceInfos) {
            PaymentAppInfo appInfo = new PaymentAppInfo();
            appInfo.caption = service.loadLabel(pm);
            appInfo.icon = service.loadIcon(pm);
            appInfo.isDefault = service.getComponent().equals(defaultApp);
            appInfo.componentName = service.getComponent();
            appInfos.add(appInfo);
        }

        return appInfos;
    }

    ComponentName getDefaultPaymentApp() {
        String componentString = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT);
        if (componentString != null) {
            return ComponentName.unflattenFromString(componentString);
        } else {
            return null;
        }
    }

    public void setDefaultPaymentApp(ComponentName app) {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT,
                app != null ? app.flattenToString() : null);
    }
}