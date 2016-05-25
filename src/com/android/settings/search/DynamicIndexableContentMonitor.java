/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.search;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.printservice.PrintServiceInfo;
import android.provider.UserDictionary;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.android.internal.content.PackageMonitor;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.print.PrintSettingsFragment;

import java.util.ArrayList;
import java.util.List;

public final class DynamicIndexableContentMonitor extends PackageMonitor implements
        InputManager.InputDeviceListener,
        LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {
    private static final String TAG = "DynamicIndexableContentMonitor";

    private static final long DELAY_PROCESS_PACKAGE_CHANGE = 2000;

    private static final int MSG_PACKAGE_AVAILABLE = 1;
    private static final int MSG_PACKAGE_UNAVAILABLE = 2;

    private final List<String> mAccessibilityServices = new ArrayList<String>();
    private final List<String> mImeServices = new ArrayList<String>();

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PACKAGE_AVAILABLE: {
                    String packageName = (String) msg.obj;
                    handlePackageAvailable(packageName);
                } break;

                case MSG_PACKAGE_UNAVAILABLE: {
                    String packageName = (String) msg.obj;
                    handlePackageUnavailable(packageName);
                } break;
            }
        }
    };

    private final ContentObserver mUserDictionaryContentObserver =
            new UserDictionaryContentObserver(mHandler);

    private Context mContext;
    private boolean mHasFeatureIme;
    private boolean mRegistered;

    private static Intent getAccessibilityServiceIntent(String packageName) {
        final Intent intent = new Intent(AccessibilityService.SERVICE_INTERFACE);
        intent.setPackage(packageName);
        return intent;
    }

    private static Intent getIMEServiceIntent(String packageName) {
        final Intent intent = new Intent("android.view.InputMethod");
        intent.setPackage(packageName);
        return intent;
    }

    public void register(Activity activity, int loaderId) {
        mContext = activity;

        if (!mContext.getSystemService(UserManager.class).isUserUnlocked()) {
            Log.w(TAG, "Skipping content monitoring because user is locked");
            mRegistered = false;
            return;
        } else {
            mRegistered = true;
        }

        boolean hasFeaturePrinting = mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_PRINTING);
        mHasFeatureIme = mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_INPUT_METHODS);

        // Cache accessibility service packages to know when they go away.
        AccessibilityManager accessibilityManager = (AccessibilityManager)
                mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> accessibilityServices = accessibilityManager
                .getInstalledAccessibilityServiceList();
        final int accessibilityServiceCount = accessibilityServices.size();
        for (int i = 0; i < accessibilityServiceCount; i++) {
            AccessibilityServiceInfo accessibilityService = accessibilityServices.get(i);
            ResolveInfo resolveInfo = accessibilityService.getResolveInfo();
            if (resolveInfo == null || resolveInfo.serviceInfo == null) {
                continue;
            }
            mAccessibilityServices.add(resolveInfo.serviceInfo.packageName);
        }

        if (hasFeaturePrinting) {
            activity.getLoaderManager().initLoader(loaderId, null, this);
        }

        // Cache IME service packages to know when they go away.
        if (mHasFeatureIme) {
            InputMethodManager imeManager = (InputMethodManager)
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> inputMethods = imeManager.getInputMethodList();
            final int inputMethodCount = inputMethods.size();
            for (int i = 0; i < inputMethodCount; i++) {
                InputMethodInfo inputMethod = inputMethods.get(i);
                ServiceInfo serviceInfo = inputMethod.getServiceInfo();
                if (serviceInfo == null) continue;
                mImeServices.add(serviceInfo.packageName);
            }

            // Watch for related content URIs.
            mContext.getContentResolver().registerContentObserver(
                    UserDictionary.Words.CONTENT_URI, true, mUserDictionaryContentObserver);
        }

        // Watch for input device changes.
        InputManager inputManager = (InputManager) activity.getSystemService(
                Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(this, mHandler);

        // Start tracking packages.
        register(activity, Looper.getMainLooper(), UserHandle.CURRENT, false);
    }

    @Override
    public void unregister() {
        if (!mRegistered) return;

        super.unregister();

        InputManager inputManager = (InputManager) mContext.getSystemService(
                Context.INPUT_SERVICE);
        inputManager.unregisterInputDeviceListener(this);

        if (mHasFeatureIme) {
            mContext.getContentResolver().unregisterContentObserver(
                    mUserDictionaryContentObserver);
        }

        mAccessibilityServices.clear();
        mImeServices.clear();
    }

    // Covers installed, appeared external storage with the package, upgraded.
    @Override
    public void onPackageAppeared(String packageName, int uid) {
        postMessage(MSG_PACKAGE_AVAILABLE, packageName);
    }

    // Covers uninstalled, removed external storage with the package.
    @Override
    public void onPackageDisappeared(String packageName, int uid) {
        postMessage(MSG_PACKAGE_UNAVAILABLE, packageName);
    }

    // Covers enabled, disabled.
    @Override
    public void onPackageModified(String packageName) {
        super.onPackageModified(packageName);
        try {
            final int state = mContext.getPackageManager().getApplicationEnabledSetting(
                    packageName);
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    || state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                postMessage(MSG_PACKAGE_AVAILABLE, packageName);
            } else {
                postMessage(MSG_PACKAGE_UNAVAILABLE, packageName);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Package does not exist: " + packageName, e);
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        Index.getInstance(mContext).updateFromClassNameResource(
                InputMethodAndLanguageSettings.class.getName(), false, true);
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        onInputDeviceChanged(deviceId);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        Index.getInstance(mContext).updateFromClassNameResource(
                InputMethodAndLanguageSettings.class.getName(), true, true);
    }

    private void postMessage(int what, String packageName) {
        Message message = mHandler.obtainMessage(what, packageName);
        mHandler.sendMessageDelayed(message, DELAY_PROCESS_PACKAGE_CHANGE);
    }

    private void handlePackageAvailable(String packageName) {
        if (!mAccessibilityServices.contains(packageName)) {
            final Intent intent = getAccessibilityServiceIntent(packageName);
            List<?> services = mContext.getPackageManager().queryIntentServices(intent, 0);
            if (services != null && !services.isEmpty()) {
                mAccessibilityServices.add(packageName);
                Index.getInstance(mContext).updateFromClassNameResource(
                        AccessibilitySettings.class.getName(), false, true);
            }
        }

        if (mHasFeatureIme) {
            if (!mImeServices.contains(packageName)) {
                Intent intent = getIMEServiceIntent(packageName);
                List<?> services = mContext.getPackageManager().queryIntentServices(intent, 0);
                if (services != null && !services.isEmpty()) {
                    mImeServices.add(packageName);
                    Index.getInstance(mContext).updateFromClassNameResource(
                            InputMethodAndLanguageSettings.class.getName(), false, true);
                }
            }
        }
    }

    private void handlePackageUnavailable(String packageName) {
        final int accessibilityIndex = mAccessibilityServices.indexOf(packageName);
        if (accessibilityIndex >= 0) {
            mAccessibilityServices.remove(accessibilityIndex);
            Index.getInstance(mContext).updateFromClassNameResource(
                    AccessibilitySettings.class.getName(), true, true);
        }

        if (mHasFeatureIme) {
            final int imeIndex = mImeServices.indexOf(packageName);
            if (imeIndex >= 0) {
                mImeServices.remove(imeIndex);
                Index.getInstance(mContext).updateFromClassNameResource(
                        InputMethodAndLanguageSettings.class.getName(), true, true);
            }
        }
    }

    @Override
    public Loader<List<PrintServiceInfo>> onCreateLoader(int id, Bundle args) {
        return new PrintServicesLoader(
                (PrintManager) mContext.getSystemService(Context.PRINT_SERVICE), mContext,
                PrintManager.ALL_SERVICES);
    }

    @Override
    public void onLoadFinished(Loader<List<PrintServiceInfo>> loader,
            List<PrintServiceInfo> services) {
        Index.getInstance(mContext).updateFromClassNameResource(
                PrintSettingsFragment.class.getName(), false, true);
    }

    @Override
    public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
        // nothing to do
    }

    private final class UserDictionaryContentObserver extends ContentObserver {

        public UserDictionaryContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (UserDictionary.Words.CONTENT_URI.equals(uri)) {
                Index.getInstance(mContext).updateFromClassNameResource(
                        InputMethodAndLanguageSettings.class.getName(), true, true);
            }
        };
    }
}
