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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DeviceConfig;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.LayoutPreference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class adds a header with device name and status (connected/disconnected, etc.).
 */
public class AdvancedBluetoothDetailsHeaderController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, OnDestroy, CachedBluetoothDevice.Callback {
    private static final String TAG = "AdvancedBtHeaderCtrl";
    private static final int LOW_BATTERY_LEVEL = 15;
    private static final int CASE_LOW_BATTERY_LEVEL = 19;
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting
    LayoutPreference mLayoutPreference;
    @VisibleForTesting
    final Map<String, Bitmap> mIconCache;
    private CachedBluetoothDevice mCachedDevice;
    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;
    @VisibleForTesting
    Handler mHandler = new Handler(Looper.getMainLooper());
    @VisibleForTesting
    boolean mIsRegisterCallback = false;
    @VisibleForTesting
    final BluetoothAdapter.OnMetadataChangedListener mMetadataListener =
            new BluetoothAdapter.OnMetadataChangedListener() {
                @Override
                public void onMetadataChanged(BluetoothDevice device, int key, byte[] value) {
                    Log.i(TAG, String.format("Metadata updated in Device %s: %d = %s.", device, key,
                            value == null ? null : new String(value)));
                    refresh();
                }
            };

    public AdvancedBluetoothDetailsHeaderController(Context context, String prefKey) {
        super(context, prefKey);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mIconCache = new HashMap<>();
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean advancedEnabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, true);
        final boolean untetheredHeadset = mCachedDevice != null
                && BluetoothUtils.getBooleanMetaData(
                mCachedDevice.getDevice(), BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET);
        Log.d(TAG, "getAvailabilityStatus() is untethered : " + untetheredHeadset);
        return advancedEnabled && untetheredHeadset ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mLayoutPreference = screen.findPreference(getPreferenceKey());
        mLayoutPreference.setVisible(isAvailable());

        refresh();
    }

    @Override
    public void onStart() {
        if (!isAvailable()) {
            return;
        }
        mIsRegisterCallback = true;
        mCachedDevice.registerCallback(this);
        mBluetoothAdapter.addOnMetadataChangedListener(mCachedDevice.getDevice(),
                mContext.getMainExecutor(), mMetadataListener);
    }

    @Override
    public void onStop() {
        if (!mIsRegisterCallback) {
            return;
        }
        mCachedDevice.unregisterCallback(this);
        mBluetoothAdapter.removeOnMetadataChangedListener(mCachedDevice.getDevice(),
                mMetadataListener);
        mIsRegisterCallback = false;
    }

    @Override
    public void onDestroy() {
        // Destroy icon bitmap associated with this header
        for (Bitmap bitmap : mIconCache.values()) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        mIconCache.clear();
    }

    public void init(CachedBluetoothDevice cachedBluetoothDevice) {
        mCachedDevice = cachedBluetoothDevice;
    }

    @VisibleForTesting
    void refresh() {
        if (mLayoutPreference != null && mCachedDevice != null) {
            final TextView title = mLayoutPreference.findViewById(R.id.entity_header_title);
            title.setText(mCachedDevice.getName());
            final TextView summary = mLayoutPreference.findViewById(R.id.entity_header_summary);
            summary.setText(mCachedDevice.getConnectionSummary(true /* shortSummary */));

            if (!mCachedDevice.isConnected() || mCachedDevice.isBusy()) {
                updateDisconnectLayout();
                return;
            }

            updateSubLayout(mLayoutPreference.findViewById(R.id.layout_left),
                    BluetoothDevice.METADATA_UNTETHERED_LEFT_ICON,
                    BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY,
                    BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING,
                    R.string.bluetooth_left_name);

            updateSubLayout(mLayoutPreference.findViewById(R.id.layout_middle),
                    BluetoothDevice.METADATA_UNTETHERED_CASE_ICON,
                    BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY,
                    BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING,
                    R.string.bluetooth_middle_name);

            updateSubLayout(mLayoutPreference.findViewById(R.id.layout_right),
                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_ICON,
                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY,
                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING,
                    R.string.bluetooth_right_name);
        }
    }

    @VisibleForTesting
    Drawable createBtBatteryIcon(Context context, int level, boolean charging) {
        final BatteryMeterView.BatteryMeterDrawable drawable =
                new BatteryMeterView.BatteryMeterDrawable(context,
                        context.getColor(R.color.meter_background_color),
                        context.getResources().getDimensionPixelSize(
                                R.dimen.advanced_bluetooth_battery_meter_width),
                        context.getResources().getDimensionPixelSize(
                                R.dimen.advanced_bluetooth_battery_meter_height));
        drawable.setBatteryLevel(level);
        drawable.setColorFilter(new PorterDuffColorFilter(
                com.android.settings.Utils.getColorAttrDefaultColor(context,
                        android.R.attr.colorControlNormal),
                PorterDuff.Mode.SRC));
        drawable.setCharging(charging);

        return drawable;
    }

    private void updateSubLayout(LinearLayout linearLayout, int iconMetaKey, int batteryMetaKey,
            int chargeMetaKey, int titleResId) {
        if (linearLayout == null) {
            return;
        }
        final BluetoothDevice bluetoothDevice = mCachedDevice.getDevice();
        final String iconUri = BluetoothUtils.getStringMetaData(bluetoothDevice, iconMetaKey);
        if (iconUri != null) {
            final ImageView imageView = linearLayout.findViewById(R.id.header_icon);
            updateIcon(imageView, iconUri);
        }

        final int batteryLevel = BluetoothUtils.getIntMetaData(bluetoothDevice, batteryMetaKey);
        final boolean charging = BluetoothUtils.getBooleanMetaData(bluetoothDevice, chargeMetaKey);
        if (DBG) {
            Log.d(TAG, "updateSubLayout() icon : " + iconMetaKey + ", battery : " + batteryMetaKey
                    + ", charge : " + chargeMetaKey + ", batteryLevel : " + batteryLevel
                    + ", charging : " + charging + ", iconUri : " + iconUri);
        }
        if (batteryLevel != BluetoothUtils.META_INT_ERROR) {
            linearLayout.setVisibility(View.VISIBLE);
            final TextView textView = linearLayout.findViewById(R.id.bt_battery_summary);
            textView.setText(com.android.settings.Utils.formatPercentage(batteryLevel));
            textView.setVisibility(View.VISIBLE);
            showBatteryIcon(linearLayout, batteryLevel, charging, batteryMetaKey);
        } else {
            // Hide it if it doesn't have battery information
            linearLayout.setVisibility(View.GONE);
        }

        final TextView textView = linearLayout.findViewById(R.id.header_title);
        textView.setText(titleResId);
        textView.setVisibility(View.VISIBLE);
    }

    private void showBatteryIcon(LinearLayout linearLayout, int level, boolean charging,
            int batteryMetaKey) {
        final int lowBatteryLevel =
                batteryMetaKey == BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY
                ? CASE_LOW_BATTERY_LEVEL : LOW_BATTERY_LEVEL;
        final boolean enableLowBattery = level <= lowBatteryLevel && !charging;
        final ImageView imageView = linearLayout.findViewById(R.id.bt_battery_icon);
        if (enableLowBattery) {
            imageView.setImageDrawable(mContext.getDrawable(R.drawable.ic_battery_alert_24dp));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    mContext.getResources().getDimensionPixelSize(
                            R.dimen.advanced_bluetooth_battery_width),
                    mContext.getResources().getDimensionPixelSize(
                            R.dimen.advanced_bluetooth_battery_height));
            layoutParams.rightMargin = mContext.getResources().getDimensionPixelSize(
                    R.dimen.advanced_bluetooth_battery_right_margin);
            imageView.setLayoutParams(layoutParams);
        } else {
            imageView.setImageDrawable(createBtBatteryIcon(mContext, level, charging));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            imageView.setLayoutParams(layoutParams);
        }
        imageView.setVisibility(View.VISIBLE);
    }

    private void updateDisconnectLayout() {
        mLayoutPreference.findViewById(R.id.layout_left).setVisibility(View.GONE);
        mLayoutPreference.findViewById(R.id.layout_right).setVisibility(View.GONE);

        // Hide title, battery icon and battery summary
        final LinearLayout linearLayout = mLayoutPreference.findViewById(R.id.layout_middle);
        linearLayout.setVisibility(View.VISIBLE);
        linearLayout.findViewById(R.id.header_title).setVisibility(View.GONE);
        linearLayout.findViewById(R.id.bt_battery_summary).setVisibility(View.GONE);
        linearLayout.findViewById(R.id.bt_battery_icon).setVisibility(View.GONE);

        // Only show bluetooth icon
        final BluetoothDevice bluetoothDevice = mCachedDevice.getDevice();
        final String iconUri = BluetoothUtils.getStringMetaData(bluetoothDevice,
                BluetoothDevice.METADATA_MAIN_ICON);
        if (DBG) {
            Log.d(TAG, "updateDisconnectLayout() iconUri : " + iconUri);
        }
        if (iconUri != null) {
            final ImageView imageView = linearLayout.findViewById(R.id.header_icon);
            updateIcon(imageView, iconUri);
        }
    }

    /**
     * Update icon by {@code iconUri}. If icon exists in cache, use it; otherwise extract it
     * from uri in background thread and update it in main thread.
     */
    @VisibleForTesting
    void updateIcon(ImageView imageView, String iconUri) {
        if (mIconCache.containsKey(iconUri)) {
            imageView.setImageBitmap(mIconCache.get(iconUri));
            return;
        }

        ThreadUtils.postOnBackgroundThread(() -> {
            final Uri uri = Uri.parse(iconUri);
            try {
                mContext.getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);

                final Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        mContext.getContentResolver(), uri);
                ThreadUtils.postOnMainThread(() -> {
                    mIconCache.put(iconUri, bitmap);
                    imageView.setImageBitmap(bitmap);
                });
            } catch (IOException e) {
                Log.e(TAG, "Failed to get bitmap for: " + iconUri, e);
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to take persistable permission for: " + uri, e);
            }
        });
    }

    @Override
    public void onDeviceAttributesChanged() {
        if (mCachedDevice != null) {
            refresh();
        }
    }
}
