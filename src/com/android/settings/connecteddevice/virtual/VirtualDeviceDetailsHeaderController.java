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

import android.companion.virtual.VirtualDevice;
import android.companion.virtual.VirtualDeviceManager;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** This class adds a header for a virtual device with a heading and icon. */
public class VirtualDeviceDetailsHeaderController extends BasePreferenceController implements
        LifecycleObserver, VirtualDeviceManager.VirtualDeviceListener {

    private static final String KEY_VIRTUAL_DEVICE_DETAILS_HEADER = "virtual_device_details_header";

    @Nullable
    private final VirtualDeviceManager mVirtualDeviceManager;
    @Nullable
    private VirtualDeviceWrapper mDevice;
    @Nullable
    private TextView mSummaryView;

    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    public VirtualDeviceDetailsHeaderController(@NonNull Context context) {
        super(context, KEY_VIRTUAL_DEVICE_DETAILS_HEADER);
        mVirtualDeviceManager =
                Objects.requireNonNull(context.getSystemService(VirtualDeviceManager.class));
    }

    /** One-time initialization when the controller is first created. */
    void init(@NonNull VirtualDeviceWrapper device) {
        mDevice = device;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onStart() {
        if (mVirtualDeviceManager != null) {
            mVirtualDeviceManager.registerVirtualDeviceListener(mExecutor, this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void onStop() {
        if (mVirtualDeviceManager != null) {
            mVirtualDeviceManager.unregisterVirtualDeviceListener(this);
        }
    }

    @Override
    public void onVirtualDeviceCreated(int deviceId) {
        VirtualDevice device =
                Objects.requireNonNull(mVirtualDeviceManager).getVirtualDevice(deviceId);
        if (mDevice != null && device != null
                && mDevice.getPersistentDeviceId().equals(device.getPersistentDeviceId())) {
            mDevice.setDeviceId(deviceId);
            mContext.getMainExecutor().execute(this::updateSummary);
        }
    }

    @Override
    public void onVirtualDeviceClosed(int deviceId) {
        if (mDevice != null && deviceId == mDevice.getDeviceId()) {
            mDevice.setDeviceId(Context.DEVICE_ID_INVALID);
            mContext.getMainExecutor().execute(this::updateSummary);
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference headerPreference = screen.findPreference(getPreferenceKey());
        View view = headerPreference.findViewById(R.id.entity_header);
        TextView titleView = view.findViewById(R.id.entity_header_title);
        ImageView iconView = headerPreference.findViewById(R.id.entity_header_icon);
        mSummaryView = view.findViewById(R.id.entity_header_summary);
        updateSummary();
        if (mDevice != null) {
            titleView.setText(mDevice.getDeviceName(mContext));
            Icon deviceIcon = android.companion.Flags.associationDeviceIcon()
                    ? mDevice.getAssociationInfo().getDeviceIcon() : null;
            if (deviceIcon == null) {
                iconView.setImageResource(R.drawable.ic_devices_other);
            } else {
                iconView.setImageIcon(deviceIcon);
            }
        }
        iconView.setContentDescription("Icon for device");
    }

    private void updateSummary() {
        if (mSummaryView != null && mDevice != null) {
            mSummaryView.setText(mDevice.getDeviceId() != Context.DEVICE_ID_INVALID
                    ? R.string.virtual_device_connected : R.string.virtual_device_disconnected);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
