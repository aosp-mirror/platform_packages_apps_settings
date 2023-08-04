/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.android.internal.util.CollectionUtils.filter;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.companion.ICompanionDeviceManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * This class adds Companion Device app rows to launch the app or remove the associations
 */
public class BluetoothDetailsCompanionAppsController extends BluetoothDetailsController {
    public static final String KEY_DEVICE_COMPANION_APPS = "device_companion_apps";
    private static final String LOG_TAG = "BTCompanionController";

    private CachedBluetoothDevice mCachedDevice;

    @VisibleForTesting
    PreferenceCategory mProfilesContainer;

    @VisibleForTesting
    CompanionDeviceManager mCompanionDeviceManager;

    @VisibleForTesting
    PackageManager mPackageManager;

    public BluetoothDetailsCompanionAppsController(Context context,
            PreferenceFragmentCompat fragment, CachedBluetoothDevice device, Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mCachedDevice = device;
        mCompanionDeviceManager = context.getSystemService(CompanionDeviceManager.class);
        mPackageManager = context.getPackageManager();
        lifecycle.addObserver(this);
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mProfilesContainer = screen.findPreference(getPreferenceKey());
        mProfilesContainer.setLayoutResource(R.layout.preference_companion_app);
    }

    private List<AssociationInfo> getAssociations(String address) {
        return filter(
                mCompanionDeviceManager.getAllAssociations(),
                a -> Objects.equal(address, a.getDeviceMacAddress()));
    }

    private static void removePreference(PreferenceCategory container, String packageName) {
        Preference preference = container.findPreference(packageName);
        if (preference != null) {
            container.removePreference(preference);
        }
    }

    private void removeAssociationDialog(String packageName, String address,
            PreferenceCategory container, CharSequence appName, Context context) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                removeAssociation(packageName, address, container);
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setPositiveButton(
                R.string.bluetooth_companion_app_remove_association_confirm_button,
                dialogClickListener)
                .setNegativeButton(android.R.string.cancel, dialogClickListener)
                .setTitle(R.string.bluetooth_companion_app_remove_association_dialog_title)
                .setMessage(mContext.getString(
                        R.string.bluetooth_companion_app_body, appName, mCachedDevice.getName()))
                .show();
    }

    private static void removeAssociation(String packageName, String address,
            PreferenceCategory container) {
        try {
            java.util.Objects.requireNonNull(ICompanionDeviceManager.Stub.asInterface(
                    ServiceManager.getService(
                            Context.COMPANION_DEVICE_SERVICE))).legacyDisassociate(
                                    address, packageName, UserHandle.myUserId());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        removePreference(container, packageName);
    }

    private CharSequence getAppName(String packageName) {
        CharSequence appName = null;
        try {
            appName = mPackageManager.getApplicationLabel(
                    mPackageManager.getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Package Not Found", e);
        }

        return appName;
    }

    private List<String> getPreferencesNeedToShow(String address, PreferenceCategory container) {
        List<String> preferencesToRemove = new ArrayList<>();
        Set<String> packages = getAssociations(address)
                .stream().map(AssociationInfo::getPackageName)
                .collect(Collectors.toSet());

        for (int i = 0; i < container.getPreferenceCount(); i++) {
            String preferenceKey = container.getPreference(i).getKey();
            if (packages.isEmpty() || !packages.contains(preferenceKey)) {
                preferencesToRemove.add(preferenceKey);
            }
        }

        for (String preferenceName : preferencesToRemove) {
            removePreference(container, preferenceName);
        }

        return packages.stream()
                .filter(p -> container.findPreference(p) == null)
                .collect(Collectors.toList());
    }

    /**
     * Refreshes the state of the preferences for all the associations, possibly adding or
     * removing preferences as needed.
     */
    @Override
    protected void refresh() {
        // Do nothing. More details in b/191992001
    }

    /**
     * Add preferences for each association for the bluetooth device
     */
    public void updatePreferences(Context context,
            String address, PreferenceCategory container) {
        // If the device is FastPair, remove CDM companion apps.
        final BluetoothFeatureProvider bluetoothFeatureProvider = FeatureFactory.getFeatureFactory()
                .getBluetoothFeatureProvider();
        final boolean sliceEnabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_SLICE_SETTINGS_ENABLED, true);
        final Uri settingsUri = bluetoothFeatureProvider.getBluetoothDeviceSettingsUri(
                mCachedDevice.getDevice());
        if (sliceEnabled && settingsUri != null) {
            container.removeAll();
            return;
        }

        Set<String> addedPackages = new HashSet<>();

        for (String packageName : getPreferencesNeedToShow(address, container)) {
            CharSequence appName = getAppName(packageName);

            if (TextUtils.isEmpty(appName) || !addedPackages.add(packageName)) {
                continue;
            }

            Drawable removeIcon = context.getResources().getDrawable(R.drawable.ic_clear);
            CompanionAppWidgetPreference preference = new CompanionAppWidgetPreference(
                    removeIcon,
                    v -> removeAssociationDialog(packageName, address, container, appName, context),
                    context
            );

            Drawable appIcon;

            try {
                appIcon = mPackageManager.getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG, "Icon Not Found", e);
                continue;
            }
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            preference.setIcon(appIcon);
            preference.setTitle(appName.toString());
            preference.setOnPreferenceClickListener(v -> {
                context.startActivity(intent);
                return true;
            });

            preference.setKey(packageName);
            preference.setVisible(true);
            container.addPreference(preference);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DEVICE_COMPANION_APPS;
    }
}
