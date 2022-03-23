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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.internal.content.PackageMonitor;

import java.util.ArrayList;
import java.util.List;

public class PaymentBackend {
    public static final String TAG = "Settings.PaymentBackend";

    public interface Callback {
        void onPaymentAppsChanged();
    }

    public static class PaymentAppInfo {
        public CharSequence label;
        CharSequence description;
        boolean isDefault;
        public ComponentName componentName;
        public ComponentName settingsComponent;
    }

    private final Context mContext;
    private final NfcAdapter mAdapter;
    private final CardEmulation mCardEmuManager;
    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();
    // Fields below only modified on UI thread
    private ArrayList<PaymentAppInfo> mAppInfos;
    private PaymentAppInfo mDefaultAppInfo;
    private ArrayList<Callback> mCallbacks = new ArrayList<>();

    public PaymentBackend(Context context) {
        mContext = context;

        mAdapter = NfcAdapter.getDefaultAdapter(context);
        mCardEmuManager = CardEmulation.getInstance(mAdapter);
        refresh();
    }

    public void onPause() {
        mSettingsPackageMonitor.unregister();
    }

    public void onResume() {
        mSettingsPackageMonitor.register(mContext, mContext.getMainLooper(), false);
        refresh();
    }

    public void refresh() {
        PackageManager pm = mContext.getPackageManager();
        List<ApduServiceInfo> serviceInfos =
                mCardEmuManager.getServices(CardEmulation.CATEGORY_PAYMENT);
        ArrayList<PaymentAppInfo> appInfos = new ArrayList<PaymentAppInfo>();

        if (serviceInfos == null) {
            makeCallbacks();
            return;
        }

        ComponentName defaultAppName = getDefaultPaymentApp();
        PaymentAppInfo foundDefaultApp = null;
        for (ApduServiceInfo service : serviceInfos) {
            PaymentAppInfo appInfo = new PaymentAppInfo();
            appInfo.label = service.loadLabel(pm);
            if (appInfo.label == null) {
                appInfo.label = service.loadAppLabel(pm);
            }
            appInfo.isDefault = service.getComponent().equals(defaultAppName);
            if (appInfo.isDefault) {
                foundDefaultApp = appInfo;
            }
            appInfo.componentName = service.getComponent();
            String settingsActivity = service.getSettingsActivityName();
            if (settingsActivity != null) {
                appInfo.settingsComponent = new ComponentName(
                        appInfo.componentName.getPackageName(),
                        settingsActivity);
            } else {
                appInfo.settingsComponent = null;
            }
            appInfo.description = service.getDescription();
            appInfos.add(appInfo);
        }
        mAppInfos = appInfos;
        mDefaultAppInfo = foundDefaultApp;
        makeCallbacks();
    }

    public void registerCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void unregisterCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    public List<PaymentAppInfo> getPaymentAppInfos() {
        return mAppInfos;
    }

    public PaymentAppInfo getDefaultApp() {
        return mDefaultAppInfo;
    }

    void makeCallbacks() {
        for (Callback callback : mCallbacks) {
            callback.onPaymentAppsChanged();
        }
    }

    boolean isForegroundMode() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.NFC_PAYMENT_FOREGROUND) != 0;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    void setForegroundMode(boolean foreground) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.NFC_PAYMENT_FOREGROUND, foreground ? 1 : 0);
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
        refresh();
    }

    private class SettingsPackageMonitor extends PackageMonitor {
        private Handler mHandler;

        @Override
        public void register(Context context, Looper thread, UserHandle user,
                boolean externalStorage) {
            if (mHandler == null) {
                mHandler = new Handler(thread) {
                    @Override
                    public void dispatchMessage(Message msg) {
                        refresh();
                    }
                };
            }
            super.register(context, thread, user, externalStorage);
        }

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
}
