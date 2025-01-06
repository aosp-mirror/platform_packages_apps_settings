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

import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Displays the list of all virtual devices. */
public class VirtualDeviceListController extends BasePreferenceController
        implements LifecycleObserver, VirtualDeviceUpdater.DeviceListener {

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    @VisibleForTesting
    VirtualDeviceUpdater mVirtualDeviceUpdater;
    @VisibleForTesting
    ArrayMap<String, Preference> mPreferences = new ArrayMap<>();
    @Nullable
    private PreferenceGroup mPreferenceGroup;
    @Nullable
    private DashboardFragment mFragment;

    public VirtualDeviceListController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mVirtualDeviceUpdater = new VirtualDeviceUpdater(context, this);
    }

    public void setFragment(@NonNull DashboardFragment fragment) {
        mFragment = fragment;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onStart() {
        if (isAvailable()) {
            mVirtualDeviceUpdater.registerListener();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void onStop() {
        if (isAvailable()) {
            mVirtualDeviceUpdater.unregisterListener();
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
        if (isAvailable()) {
            mVirtualDeviceUpdater.loadDevices();
        }
    }

    @Override
    public void onDeviceAdded(@NonNull VirtualDeviceWrapper device) {
        Preference preference = new Preference(mContext);
        CharSequence deviceName = device.getDeviceName(mContext);
        preference.setTitle(deviceName);
        preference.setKey(device.getPersistentDeviceId() + "_" + deviceName);
        final CharSequence title = preference.getTitle();

        Icon deviceIcon = android.companion.Flags.associationDeviceIcon()
                ? device.getAssociationInfo().getDeviceIcon() : null;
        if (deviceIcon == null) {
            preference.setIcon(R.drawable.ic_devices_other);
        } else {
            preference.setIcon(deviceIcon.loadDrawable(mContext));
        }
        if (device.getDeviceId() != Context.DEVICE_ID_INVALID) {
            preference.setSummary(R.string.virtual_device_connected);
        } else {
            preference.setSummary(R.string.virtual_device_disconnected);
        }

        preference.setOnPreferenceClickListener((Preference p) -> {
            mMetricsFeatureProvider.logClickedPreference(p, getMetricsCategory());
            final Bundle args = new Bundle();
            args.putParcelable(VirtualDeviceDetailsFragment.DEVICE_ARG, device);
            if (mFragment != null) {
                new SubSettingLauncher(mFragment.getContext())
                        .setDestination(VirtualDeviceDetailsFragment.class.getName())
                        .setTitleText(title)
                        .setArguments(args)
                        .setSourceMetricsCategory(getMetricsCategory())
                        .launch();
            }
            return true;
        });
        mPreferences.put(device.getPersistentDeviceId(), preference);
        if (mPreferenceGroup != null) {
            mContext.getMainExecutor().execute(() ->
                    Objects.requireNonNull(mPreferenceGroup).addPreference(preference));
        }
    }

    @Override
    public void onDeviceRemoved(@NonNull VirtualDeviceWrapper device) {
        Preference preference = mPreferences.remove(device.getPersistentDeviceId());
        if (mPreferenceGroup != null) {
            mContext.getMainExecutor().execute(() ->
                    Objects.requireNonNull(mPreferenceGroup).removePreference(preference));
        }
    }

    @Override
    public void onDeviceChanged(@NonNull VirtualDeviceWrapper device) {
        Preference preference = mPreferences.get(device.getPersistentDeviceId());
        if (preference != null) {
            int summaryResId = device.getDeviceId() != Context.DEVICE_ID_INVALID
                    ? R.string.virtual_device_connected : R.string.virtual_device_disconnected;
            mContext.getMainExecutor().execute(() ->
                    Objects.requireNonNull(preference).setSummary(summaryResId));
        }
    }

    @Override
    public void updateDynamicRawDataToIndex(@NonNull List<SearchIndexableRaw> rawData) {
        if (!isAvailable()) {
            return;
        }
        Collection<VirtualDeviceWrapper> devices = mVirtualDeviceUpdater.loadDevices();
        for (VirtualDeviceWrapper device : devices) {
            SearchIndexableRaw data = new SearchIndexableRaw(mContext);
            String deviceName = device.getDeviceName(mContext).toString();
            data.key = device.getPersistentDeviceId() + "_" + deviceName;
            data.title = deviceName;
            data.summaryOn = mContext.getString(R.string.connected_device_connections_title);
            rawData.add(data);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_enableVirtualDeviceManager)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!android.companion.virtualdevice.flags.Flags.vdmSettings()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }
}
