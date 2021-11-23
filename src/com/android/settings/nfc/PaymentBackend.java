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

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
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
        public UserHandle userHandle;
    }

    /**
     * ComponentName of the payment application and the userId that it belongs to.
     */
    public static class PaymentInfo {
        public ComponentName componentName;
        public int userId;
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
        ArrayList<PaymentAppInfo> appInfosAllProfiles = new ArrayList<PaymentAppInfo>();

        UserManager um = mContext.createContextAsUser(
                UserHandle.of(ActivityManager.getCurrentUser()), /*flags=*/0)
                .getSystemService(UserManager.class);
        List<UserHandle> userHandles = um.getEnabledProfiles();

        PaymentInfo defaultAppName = getDefaultPaymentApp();
        PaymentAppInfo foundDefaultApp = null;
        for (UserHandle uh : userHandles) {
            List<ApduServiceInfo> serviceInfosByProfile =
                    mCardEmuManager.getServices(CardEmulation.CATEGORY_PAYMENT, uh.getIdentifier());
            if (serviceInfosByProfile == null) continue;

            ArrayList<PaymentAppInfo> appInfos = new ArrayList<PaymentAppInfo>();

            for (ApduServiceInfo service : serviceInfosByProfile) {
                PaymentAppInfo appInfo = new PaymentAppInfo();
                appInfo.userHandle = uh;
                appInfo.label = service.loadLabel(pm);
                if (appInfo.label == null) {
                    appInfo.label = service.loadAppLabel(pm);
                }
                if (defaultAppName == null) {
                    appInfo.isDefault = false;
                } else {
                    appInfo.isDefault =
                            service.getComponent().equals(defaultAppName.componentName)
                            && defaultAppName.userId == uh.getIdentifier();
                }
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
            appInfosAllProfiles.addAll(appInfos);
        }
        mAppInfos = appInfosAllProfiles;
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
            return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                    Settings.Secure.NFC_PAYMENT_FOREGROUND, UserHandle.myUserId()) != 0;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    void setForegroundMode(boolean foreground) {
        UserManager um = mContext.createContextAsUser(
                UserHandle.of(UserHandle.myUserId()), /*flags=*/0)
                .getSystemService(UserManager.class);
        List<UserHandle> userHandles = um.getEnabledProfiles();
        for (UserHandle uh : userHandles) {
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    Settings.Secure.NFC_PAYMENT_FOREGROUND, foreground ? 1 : 0, uh.getIdentifier());
        }
    }

    PaymentInfo getDefaultPaymentApp() {
        UserManager um = mContext.createContextAsUser(
                UserHandle.of(ActivityManager.getCurrentUser()), /*flags=*/0)
                .getSystemService(UserManager.class);
        List<UserHandle> userHandles = um.getEnabledProfiles();
        for (UserHandle uh : userHandles) {
            ComponentName defaultApp = getDefaultPaymentApp(uh.getIdentifier());
            if (defaultApp != null) {
                PaymentInfo appInfo = new PaymentInfo();
                appInfo.userId = uh.getIdentifier();
                appInfo.componentName = defaultApp;
                return appInfo;
            }
        }
        return null;
    }

    ComponentName getDefaultPaymentApp(int userId) {
        String componentString = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT, userId);
        if (componentString != null) {
            return ComponentName.unflattenFromString(componentString);
        } else {
            return null;
        }
    }

    public void setDefaultPaymentApp(ComponentName app) {
        setDefaultPaymentApp(app, UserHandle.myUserId());
    }

    /**
     *  Set Nfc default payment application
     */
    public void setDefaultPaymentApp(ComponentName app, int userId) {
        UserManager um = mContext.createContextAsUser(
                UserHandle.of(ActivityManager.getCurrentUser()), /*flags=*/0)
                .getSystemService(UserManager.class);
        List<UserHandle> userHandles = um.getEnabledProfiles();

        for (UserHandle uh : userHandles) {
            if (uh.getIdentifier() == userId) {
                Settings.Secure.putStringForUser(mContext.getContentResolver(),
                        Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT,
                        app != null ? app.flattenToString() : null, uh.getIdentifier());
            } else {
                Settings.Secure.putStringForUser(mContext.getContentResolver(),
                        Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT,
                        null, uh.getIdentifier());
            }
        }
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
