/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.addappnetworks;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;
import com.android.settingslib.wifi.WifiEnterpriseRestrictionUtils;

/**
 * When apps send a new intent with a WifiConfiguration list extra to Settings APP. Settings APP
 * will launch this activity, which contains {@code AddAppNetworksFragment}, with a UI panel pop
 * up asking the user if they approve the network being proposed by the app to be saved on to
 * the device. User can decide to save or cancel the request.,
 */
public class AddAppNetworksActivity extends FragmentActivity {
    public static final String TAG = "AddAppNetworksActivity";

    private AddAppNetworksFragment mFragment;

    /** Key specifying the package name of the apps which requested the Panel. */
    public static final String KEY_CALLING_PACKAGE_NAME = "panel_calling_package_name";

    @VisibleForTesting
    final Bundle mBundle = new Bundle();
    @VisibleForTesting
    IActivityManager mActivityManager = ActivityManager.getService();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_panel);
        if (!showAddNetworksFragment()) {
            finish();
            return;
        }
        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));

        // Move the window to the bottom of screen, and make it take up the entire screen width.
        final Window window = getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!showAddNetworksFragment()) {
            finish();
            return;
        }
    }

    @VisibleForTesting
    boolean showAddNetworksFragment() {
        if (isGuestUser(getApplicationContext())) {
            Log.e(TAG, "Guest user is not allowed to configure Wi-Fi!");
            EventLog.writeEvent(0x534e4554, "224772678", -1 /* UID */, "User is a guest");
            return false;
        }

        if (!isAddWifiConfigAllow()) {
            Log.d(TAG, "Not allowed by Enterprise Restriction");
            return false;
        }
        String packageName = getCallingAppPackageName();
        if (TextUtils.isEmpty(packageName)) {
            Log.d(TAG, "Package name is null");
            return false;
        }

        // TODO: Check the new intent status.
        mBundle.putString(KEY_CALLING_PACKAGE_NAME, packageName);
        mBundle.putParcelableArrayList(Settings.EXTRA_WIFI_NETWORK_LIST,
                getIntent().getParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST));

        final FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new AddAppNetworksFragment();
            fragment.setArguments(mBundle);
            fragmentManager.beginTransaction().add(R.id.main_content, fragment, TAG).commit();
        } else {
            ((AddAppNetworksFragment) fragment).createContent(mBundle);
        }

        return true;
    }

    @VisibleForTesting
    protected String getCallingAppPackageName() {
        String packageName;
        try {
            packageName = mActivityManager.getLaunchedFromPackage(getActivityToken());
        } catch (RemoteException e) {
            Log.e(TAG, "Can not get the package from activity manager");
            return null;
        }
        return packageName;
    }

    @VisibleForTesting
    boolean isAddWifiConfigAllow() {
        return WifiEnterpriseRestrictionUtils.isAddWifiConfigAllowed(this);
    }

    private static boolean isGuestUser(Context context) {
        if (context == null) return false;
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) return false;
        return userManager.isGuestUser();
    }
}
