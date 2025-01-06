/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.connecteddevice.virtual;

import static com.android.settingslib.drawer.TileUtils.IA_SETTINGS_ACTION;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.companion.virtual.VirtualDevice;
import android.companion.virtual.VirtualDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.google.common.collect.ImmutableSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Maintains a collection of all virtual devices and propagates any changes to its listener. */
class VirtualDeviceUpdater implements VirtualDeviceManager.VirtualDeviceListener {

    private static final String CDM_PERSISTENT_DEVICE_ID_PREFIX = "companion:";

    // TODO(b/384400670): Detect these packages via PackageManager instead of hardcoding them.
    private static final ImmutableSet<String> IGNORED_PACKAGES =
            ImmutableSet.of("com.google.ambient.streaming");

    private final VirtualDeviceManager mVirtualDeviceManager;
    private final CompanionDeviceManager mCompanionDeviceManager;
    private final PackageManager mPackageManager;
    private final DeviceListener mDeviceListener;
    private final Executor mBackgroundExecutor = Executors.newSingleThreadExecutor();

    // Up-to-date list of active and inactive devices, keyed by persistent device id.
    @VisibleForTesting
    ArrayMap<String, VirtualDeviceWrapper> mDevices = new ArrayMap<>();

    interface DeviceListener {
        void onDeviceAdded(@NonNull VirtualDeviceWrapper device);
        void onDeviceRemoved(@NonNull VirtualDeviceWrapper device);
        void onDeviceChanged(@NonNull VirtualDeviceWrapper device);
    }

    VirtualDeviceUpdater(Context context, DeviceListener deviceListener) {
        mVirtualDeviceManager = context.getSystemService(VirtualDeviceManager.class);
        mCompanionDeviceManager = context.getSystemService(CompanionDeviceManager.class);
        mPackageManager = context.getPackageManager();
        mDeviceListener = deviceListener;
    }

    void registerListener() {
        mVirtualDeviceManager.registerVirtualDeviceListener(mBackgroundExecutor, this);
        mBackgroundExecutor.execute(this::loadDevices);
    }

    void unregisterListener() {
        mVirtualDeviceManager.unregisterVirtualDeviceListener(this);
    }

    @Override
    public void onVirtualDeviceCreated(int deviceId) {
        loadDevices();
    }

    @Override
    public void onVirtualDeviceClosed(int deviceId) {
        loadDevices();
    }

    Collection<VirtualDeviceWrapper> loadDevices() {
        final Set<String> persistentDeviceIds = mVirtualDeviceManager.getAllPersistentDeviceIds();
        final Set<String> deviceIdsToRemove = new ArraySet<>();
        for (String persistentDeviceId : mDevices.keySet()) {
            if (!persistentDeviceIds.contains(persistentDeviceId)) {
                deviceIdsToRemove.add(persistentDeviceId);
            }
        }
        for (String persistentDeviceId : deviceIdsToRemove) {
            mDeviceListener.onDeviceRemoved(mDevices.remove(persistentDeviceId));
        }

        if (!persistentDeviceIds.isEmpty()) {
            for (VirtualDevice device : mVirtualDeviceManager.getVirtualDevices()) {
                String persistentDeviceId = device.getPersistentDeviceId();
                persistentDeviceIds.remove(persistentDeviceId);
                addOrUpdateDevice(persistentDeviceId, device.getDeviceId());
            }
        }

        for (String persistentDeviceId : persistentDeviceIds) {
            addOrUpdateDevice(persistentDeviceId, Context.DEVICE_ID_INVALID);
        }

        return mDevices.values();
    }

    private void addOrUpdateDevice(String persistentDeviceId, int deviceId) {
        VirtualDeviceWrapper device = mDevices.get(persistentDeviceId);
        if (device == null) {
            AssociationInfo associationInfo = getAssociationInfo(persistentDeviceId);
            if (associationInfo == null) {
                return;
            }
            device = new VirtualDeviceWrapper(associationInfo, persistentDeviceId, deviceId);
            mDevices.put(persistentDeviceId, device);
            mDeviceListener.onDeviceAdded(device);
        }
        if (device.getDeviceId() != deviceId) {
            device.setDeviceId(deviceId);
            mDeviceListener.onDeviceChanged(device);
        }
    }

    @Nullable
    private AssociationInfo getAssociationInfo(String persistentDeviceId) {
        if (persistentDeviceId == null) {
            return null;
        }
        VirtualDeviceWrapper device = mDevices.get(persistentDeviceId);
        if (device != null) {
            return device.getAssociationInfo();
        }
        if (!persistentDeviceId.startsWith(CDM_PERSISTENT_DEVICE_ID_PREFIX)) {
            return null;
        }
        final int associationId = Integer.parseInt(
                persistentDeviceId.replaceFirst(CDM_PERSISTENT_DEVICE_ID_PREFIX, ""));
        final List<AssociationInfo> associations =
                mCompanionDeviceManager.getAllAssociations(UserHandle.USER_ALL);
        final AssociationInfo associationInfo = associations.stream()
                .filter(a -> a.getId() == associationId)
                .findFirst()
                .orElse(null);
        if (associationInfo == null) {
            return null;
        }
        if (shouldExcludePackageFromSettings(associationInfo.getPackageName())) {
            return null;
        }
        return associationInfo;
    }

    // Some packages already inject custom settings entries that allow the users to manage the
    // virtual devices and the companion associations, so they should be ignored from the generic
    // settings page.
    private boolean shouldExcludePackageFromSettings(String packageName) {
        if (packageName == null || IGNORED_PACKAGES.contains(packageName)) {
            return true;
        }
        final Intent intent = new Intent(IA_SETTINGS_ACTION);
        intent.setPackage(packageName);
        return intent.resolveActivity(mPackageManager) != null;
    }
}
