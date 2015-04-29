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

import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import com.android.internal.content.PackageMonitor;

import java.util.ArrayList;
import java.util.List;

public class PaymentBackend {
    public static final String TAG = "Settings.PaymentBackend";

    public interface Callback {
        void onPaymentAppsChanged();
    }

    public static class PaymentAppInfo {
        CharSequence label;
        CharSequence description;
        Drawable banner;
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
    private ArrayList<Callback> mCallbacks = new ArrayList<Callback>();

    public PaymentBackend(Context context) {
        mContext = context;

        mAdapter = NfcAdapter.getDefaultAdapter(context);
        mCardEmuManager = CardEmulation.getInstance(mAdapter);
        refresh();
    }

    public void onPause() {
        mSettingsPackageMonitor.unregister();
        mContext.unregisterReceiver(mReceiver);
    }

    public void onResume() {
        mSettingsPackageMonitor.register(mContext, mContext.getMainLooper(), false);
        // Register broadcast receiver for dynamic resource updates
        IntentFilter filter = new IntentFilter(CardEmulation.ACTION_REQUEST_SERVICE_RESOURCES);
        mContext.registerReceiver(mReceiver, filter);
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
                appInfo.settingsComponent = new ComponentName(appInfo.componentName.getPackageName(),
                        settingsActivity);
            } else {
                appInfo.settingsComponent = null;
            }
            if (service.hasDynamicResources()) {
                appInfo.description = "";
                appInfo.banner = null;
                sendBroadcastForResources(appInfo);
            } else {
                appInfo.description = service.getDescription();
                appInfo.banner = service.loadBanner(pm);
            }
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

    Drawable loadDrawableForPackage(String pkgName, int drawableResId) {
        PackageManager pm = mContext.getPackageManager();
        try {
            Resources res = pm.getResourcesForApplication(pkgName);
            Drawable banner = res.getDrawable(drawableResId);
            return banner;
        } catch (Resources.NotFoundException e) {
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    void sendBroadcastForResources(PaymentAppInfo appInfo) {
        Intent broadcastIntent = new Intent(CardEmulation.ACTION_REQUEST_SERVICE_RESOURCES);
        broadcastIntent.setPackage(appInfo.componentName.getPackageName());
        broadcastIntent.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, appInfo.componentName);
        mContext.sendOrderedBroadcastAsUser(broadcastIntent, UserHandle.CURRENT,
                null, mReceiver, null, Activity.RESULT_OK, null, null);
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
                Settings.Secure.NFC_PAYMENT_FOREGROUND, foreground ? 1 : 0) ;
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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle results = getResultExtras(false);
            if (results != null) {
                String desc = results.getString(CardEmulation.EXTRA_DESCRIPTION);
                int resId = results.getInt(CardEmulation.EXTRA_BANNER_RES_ID, -1);
                // Find corresponding component
                PaymentAppInfo matchingAppInfo = null;
                for (PaymentAppInfo appInfo : mAppInfos) {
                    if (appInfo.componentName.equals(
                            intent.getParcelableExtra(CardEmulation.EXTRA_SERVICE_COMPONENT))) {
                        matchingAppInfo = appInfo;
                    }
                }
                if (matchingAppInfo != null && (desc != null || resId != -1)) {
                    if (desc != null) {
                        matchingAppInfo.description = desc;
                    }
                    if (resId != -1) {
                        matchingAppInfo.banner = loadDrawableForPackage(
                                matchingAppInfo.componentName.getPackageName(), resId);
                    }
                    makeCallbacks();
                }
            } else {
                Log.e(TAG, "Didn't find results extra.");
            }

        }
    };
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
}