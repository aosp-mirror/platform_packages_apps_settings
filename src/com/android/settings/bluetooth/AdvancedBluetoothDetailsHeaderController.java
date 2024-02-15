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

import static com.android.settings.bluetooth.Utils.preloadAndRun;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.LayoutPreference;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class adds a header with device name and status (connected/disconnected, etc.).
 */
public class AdvancedBluetoothDetailsHeaderController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, OnDestroy, CachedBluetoothDevice.Callback {
    private static final String TAG = "AdvancedBtHeaderCtrl";
    private static final int LOW_BATTERY_LEVEL = 15;
    private static final int CASE_LOW_BATTERY_LEVEL = 19;
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String PATH = "time_remaining";
    private static final String QUERY_PARAMETER_ADDRESS = "address";
    private static final String QUERY_PARAMETER_BATTERY_ID = "battery_id";
    private static final String QUERY_PARAMETER_BATTERY_LEVEL = "battery_level";
    private static final String QUERY_PARAMETER_TIMESTAMP = "timestamp";
    private static final String BATTERY_ESTIMATE = "battery_estimate";
    private static final String ESTIMATE_READY = "estimate_ready";
    private static final String DATABASE_ID = "id";
    private static final String DATABASE_BLUETOOTH = "Bluetooth";
    private static final long TIME_OF_HOUR = TimeUnit.SECONDS.toMillis(3600);
    private static final long TIME_OF_MINUTE = TimeUnit.SECONDS.toMillis(60);
    private static final int LEFT_DEVICE_ID = 1;
    private static final int RIGHT_DEVICE_ID = 2;
    private static final int CASE_DEVICE_ID = 3;
    private static final int MAIN_DEVICE_ID = 4;
    private static final float HALF_ALPHA = 0.5f;

    @VisibleForTesting
    LayoutPreference mLayoutPreference;
    @VisibleForTesting
    final Map<String, Bitmap> mIconCache;
    private CachedBluetoothDevice mCachedDevice;
    private Set<BluetoothDevice> mBluetoothDevices;
    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;
    @VisibleForTesting
    Handler mHandler = new Handler(Looper.getMainLooper());
    @VisibleForTesting
    boolean mIsLeftDeviceEstimateReady;
    @VisibleForTesting
    boolean mIsRightDeviceEstimateReady;
    @VisibleForTesting
    final BluetoothAdapter.OnMetadataChangedListener mMetadataListener =
            new BluetoothAdapter.OnMetadataChangedListener() {
                @Override
                public void onMetadataChanged(BluetoothDevice device, int key, byte[] value) {
                    Log.d(TAG, String.format("Metadata updated in Device %s: %d = %s.",
                            device.getAnonymizedAddress(),
                            key, value == null ? null : new String(value)));
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
        if (mCachedDevice == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return BluetoothUtils.isAdvancedDetailsHeader(mCachedDevice.getDevice())
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mLayoutPreference = screen.findPreference(getPreferenceKey());
        mLayoutPreference.setVisible(isAvailable());
    }

    @Override
    public void onStart() {
        if (!isAvailable()) {
            return;
        }
        registerBluetoothDevice();
        refresh();
    }

    @Override
    public void onStop() {
        unRegisterBluetoothDevice();
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

    private void registerBluetoothDevice() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "No mBluetoothAdapter");
            return;
        }
        if (mBluetoothDevices == null) {
            mBluetoothDevices = new HashSet<>();
        }
        mBluetoothDevices.clear();
        if (mCachedDevice.getDevice() != null) {
            mBluetoothDevices.add(mCachedDevice.getDevice());
        }
        mCachedDevice.getMemberDevice().forEach(cbd -> {
            if (cbd != null) {
                mBluetoothDevices.add(cbd.getDevice());
            }
        });
        if (mBluetoothDevices.isEmpty()) {
            Log.d(TAG, "No BT device to register.");
            return;
        }
        mCachedDevice.registerCallback(this);
        Set<BluetoothDevice> errorDevices = new HashSet<>();
        mBluetoothDevices.forEach(bd -> {
            try {
                boolean isSuccess = mBluetoothAdapter.addOnMetadataChangedListener(bd,
                        mContext.getMainExecutor(), mMetadataListener);
                if (!isSuccess) {
                    Log.e(TAG, bd.getAnonymizedAddress() + ": add into Listener failed");
                    errorDevices.add(bd);
                }
            } catch (NullPointerException e) {
                errorDevices.add(bd);
                Log.e(TAG, bd.getAnonymizedAddress() + ":" + e.toString());
            } catch (IllegalArgumentException e) {
                errorDevices.add(bd);
                Log.e(TAG, bd.getAnonymizedAddress() + ":" + e.toString());
            }
        });
        for (BluetoothDevice errorDevice : errorDevices) {
            mBluetoothDevices.remove(errorDevice);
            Log.d(TAG, "mBluetoothDevices remove " + errorDevice.getAnonymizedAddress());
        }
    }

    private void unRegisterBluetoothDevice() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "No mBluetoothAdapter");
            return;
        }
        if (mBluetoothDevices == null || mBluetoothDevices.isEmpty()) {
            Log.d(TAG, "No BT device to unregister.");
            return;
        }
        mCachedDevice.unregisterCallback(this);
        mBluetoothDevices.forEach(bd -> {
            try {
                mBluetoothAdapter.removeOnMetadataChangedListener(bd, mMetadataListener);
            } catch (NullPointerException e) {
                Log.e(TAG, bd.getAnonymizedAddress() + ":" + e.toString());
            } catch (IllegalArgumentException e) {
                Log.e(TAG, bd.getAnonymizedAddress() + ":" + e.toString());
            }
        });
        mBluetoothDevices.clear();
    }

    @VisibleForTesting
    void refresh() {
        if (mLayoutPreference != null && mCachedDevice != null) {
            Supplier<String> deviceName = Suppliers.memoize(() -> mCachedDevice.getName());
            Supplier<Boolean> disconnected =
                    Suppliers.memoize(() -> !mCachedDevice.isConnected() || mCachedDevice.isBusy());
            Supplier<Boolean> isUntetheredHeadset =
                    Suppliers.memoize(() -> isUntetheredHeadset(mCachedDevice.getDevice()));
            Supplier<String> summaryText =
                    Suppliers.memoize(
                            () -> {
                                if (disconnected.get() || isUntetheredHeadset.get()) {
                                    return mCachedDevice.getConnectionSummary(
                                            /* shortSummary= */ true);
                                }
                                return mCachedDevice.getConnectionSummary(
                                        BluetoothUtils.getIntMetaData(
                                                        mCachedDevice.getDevice(),
                                                        BluetoothDevice.METADATA_MAIN_BATTERY)
                                                != BluetoothUtils.META_INT_ERROR);
                            });
            preloadAndRun(
                    List.of(deviceName, disconnected, isUntetheredHeadset, summaryText),
                    () -> {
                        final TextView title =
                                mLayoutPreference.findViewById(R.id.entity_header_title);
                        title.setText(deviceName.get());
                        final TextView summary =
                                mLayoutPreference.findViewById(R.id.entity_header_summary);

                        if (disconnected.get()) {
                            summary.setText(summaryText.get());
                            updateDisconnectLayout();
                            return;
                        }
                        if (isUntetheredHeadset.get()) {
                            summary.setText(summaryText.get());
                            updateSubLayout(
                                    mLayoutPreference.findViewById(R.id.layout_left),
                                    BluetoothDevice.METADATA_UNTETHERED_LEFT_ICON,
                                    BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY,
                                    BluetoothDevice.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
                                    BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING,
                                    R.string.bluetooth_left_name,
                                    LEFT_DEVICE_ID);

                            updateSubLayout(
                                    mLayoutPreference.findViewById(R.id.layout_middle),
                                    BluetoothDevice.METADATA_UNTETHERED_CASE_ICON,
                                    BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY,
                                    BluetoothDevice.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
                                    BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING,
                                    R.string.bluetooth_middle_name,
                                    CASE_DEVICE_ID);

                            updateSubLayout(
                                    mLayoutPreference.findViewById(R.id.layout_right),
                                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_ICON,
                                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY,
                                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
                                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING,
                                    R.string.bluetooth_right_name,
                                    RIGHT_DEVICE_ID);

                            showBothDevicesBatteryPredictionIfNecessary();
                        } else {
                            mLayoutPreference
                                    .findViewById(R.id.layout_left)
                                    .setVisibility(View.GONE);
                            mLayoutPreference
                                    .findViewById(R.id.layout_right)
                                    .setVisibility(View.GONE);

                            summary.setText(summaryText.get());
                            updateSubLayout(
                                    mLayoutPreference.findViewById(R.id.layout_middle),
                                    BluetoothDevice.METADATA_MAIN_ICON,
                                    BluetoothDevice.METADATA_MAIN_BATTERY,
                                    BluetoothDevice.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                                    BluetoothDevice.METADATA_MAIN_CHARGING,
                                    /* titleResId= */ 0,
                                    MAIN_DEVICE_ID);
                        }
                    });
        }
    }

    @VisibleForTesting
    Drawable createBtBatteryIcon(Context context, int level, boolean charging) {
        final BatteryMeterView.BatteryMeterDrawable drawable =
                new BatteryMeterView.BatteryMeterDrawable(context,
                        context.getColor(com.android.settingslib.R.color.meter_background_color),
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

    private void updateSubLayout(
            LinearLayout linearLayout,
            int iconMetaKey,
            int batteryMetaKey,
            int lowBatteryMetaKey,
            int chargeMetaKey,
            int titleResId,
            int deviceId) {
        if (linearLayout == null) {
            return;
        }
        BluetoothDevice bluetoothDevice = mCachedDevice.getDevice();
        Supplier<String> iconUri =
                Suppliers.memoize(
                        () -> BluetoothUtils.getStringMetaData(bluetoothDevice, iconMetaKey));
        Supplier<Integer> batteryLevel =
                Suppliers.memoize(
                        () -> BluetoothUtils.getIntMetaData(bluetoothDevice, batteryMetaKey));
        Supplier<Boolean> charging =
                Suppliers.memoize(
                        () -> BluetoothUtils.getBooleanMetaData(bluetoothDevice, chargeMetaKey));
        Supplier<Integer> lowBatteryLevel =
                Suppliers.memoize(
                        () -> {
                            int level =
                                    BluetoothUtils.getIntMetaData(
                                            bluetoothDevice, lowBatteryMetaKey);
                            if (level == BluetoothUtils.META_INT_ERROR) {
                                if (batteryMetaKey
                                        == BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY) {
                                    level = CASE_LOW_BATTERY_LEVEL;
                                } else {
                                    level = LOW_BATTERY_LEVEL;
                                }
                            }
                            return level;
                        });
        Supplier<Boolean> isUntethered =
                Suppliers.memoize(() -> isUntetheredHeadset(bluetoothDevice));
        Supplier<Integer> nativeBatteryLevel = Suppliers.memoize(bluetoothDevice::getBatteryLevel);
        preloadAndRun(
                List.of(
                        iconUri,
                        batteryLevel,
                        charging,
                        lowBatteryLevel,
                        isUntethered,
                        nativeBatteryLevel),
                () ->
                        updateSubLayoutUi(
                                linearLayout,
                                iconMetaKey,
                                batteryMetaKey,
                                lowBatteryMetaKey,
                                chargeMetaKey,
                                titleResId,
                                deviceId,
                                iconUri,
                                batteryLevel,
                                charging,
                                lowBatteryLevel,
                                isUntethered,
                                nativeBatteryLevel));
    }

    private void updateSubLayoutUi(
            LinearLayout linearLayout,
            int iconMetaKey,
            int batteryMetaKey,
            int lowBatteryMetaKey,
            int chargeMetaKey,
            int titleResId,
            int deviceId,
            Supplier<String> preloadedIconUri,
            Supplier<Integer> preloadedBatteryLevel,
            Supplier<Boolean> preloadedCharging,
            Supplier<Integer> preloadedLowBatteryLevel,
            Supplier<Boolean> preloadedIsUntethered,
            Supplier<Integer> preloadedNativeBatteryLevel) {
        final BluetoothDevice bluetoothDevice = mCachedDevice.getDevice();
        final String iconUri = preloadedIconUri.get();
        final ImageView imageView = linearLayout.findViewById(R.id.header_icon);
        if (iconUri != null) {
            updateIcon(imageView, iconUri);
        } else {
            final Pair<Drawable, String> pair =
                    BluetoothUtils.getBtRainbowDrawableWithDescription(mContext, mCachedDevice);
            imageView.setImageDrawable(pair.first);
            imageView.setContentDescription(pair.second);
        }
        final int batteryLevel = preloadedBatteryLevel.get();
        final boolean charging = preloadedCharging.get();
        int lowBatteryLevel = preloadedLowBatteryLevel.get();

        Log.d(TAG, "buletoothDevice: " + bluetoothDevice.getAnonymizedAddress()
                + ", updateSubLayout() icon : " + iconMetaKey + ", battery : " + batteryMetaKey
                + ", charge : " + chargeMetaKey + ", batteryLevel : " + batteryLevel
                + ", charging : " + charging + ", iconUri : " + iconUri
                + ", lowBatteryLevel : " + lowBatteryLevel);

        if (deviceId == LEFT_DEVICE_ID || deviceId == RIGHT_DEVICE_ID) {
            showBatteryPredictionIfNecessary(linearLayout, deviceId, batteryLevel);
        }
        final TextView batterySummaryView = linearLayout.findViewById(R.id.bt_battery_summary);
        if (preloadedIsUntethered.get()) {
            if (batteryLevel != BluetoothUtils.META_INT_ERROR) {
                linearLayout.setVisibility(View.VISIBLE);
                batterySummaryView.setText(
                        com.android.settings.Utils.formatPercentage(batteryLevel));
                batterySummaryView.setVisibility(View.VISIBLE);
                showBatteryIcon(linearLayout, batteryLevel, lowBatteryLevel, charging);
            } else {
                if (deviceId == MAIN_DEVICE_ID) {
                    linearLayout.setVisibility(View.VISIBLE);
                    linearLayout.findViewById(R.id.bt_battery_icon).setVisibility(View.GONE);
                    int level = preloadedNativeBatteryLevel.get();
                    if (level != BluetoothDevice.BATTERY_LEVEL_UNKNOWN
                            && level != BluetoothDevice.BATTERY_LEVEL_BLUETOOTH_OFF) {
                        batterySummaryView.setText(
                                com.android.settings.Utils.formatPercentage(level));
                        batterySummaryView.setVisibility(View.VISIBLE);
                    } else {
                        batterySummaryView.setVisibility(View.GONE);
                    }
                } else {
                    // Hide it if it doesn't have battery information
                    linearLayout.setVisibility(View.GONE);
                }
            }
        } else {
            if (batteryLevel != BluetoothUtils.META_INT_ERROR) {
                linearLayout.setVisibility(View.VISIBLE);
                batterySummaryView.setText(
                        com.android.settings.Utils.formatPercentage(batteryLevel));
                batterySummaryView.setVisibility(View.VISIBLE);
                showBatteryIcon(linearLayout, batteryLevel, lowBatteryLevel, charging);
            } else {
                batterySummaryView.setVisibility(View.GONE);
            }
        }
        final TextView textView = linearLayout.findViewById(R.id.header_title);
        if (deviceId == MAIN_DEVICE_ID) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(titleResId);
            textView.setVisibility(View.VISIBLE);
        }
    }

    private boolean isUntetheredHeadset(BluetoothDevice bluetoothDevice) {
        return BluetoothUtils.getBooleanMetaData(bluetoothDevice,
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)
                || TextUtils.equals(BluetoothUtils.getStringMetaData(bluetoothDevice,
                BluetoothDevice.METADATA_DEVICE_TYPE),
                BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET);
    }

    private void showBatteryPredictionIfNecessary(LinearLayout linearLayout, int batteryId,
            int batteryLevel) {
        ThreadUtils.postOnBackgroundThread(() -> {
            final Uri contentUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(mContext.getString(R.string.config_battery_prediction_authority))
                    .appendPath(PATH)
                    .appendPath(DATABASE_ID)
                    .appendPath(DATABASE_BLUETOOTH)
                    .appendQueryParameter(QUERY_PARAMETER_ADDRESS, mCachedDevice.getAddress())
                    .appendQueryParameter(QUERY_PARAMETER_BATTERY_ID, String.valueOf(batteryId))
                    .appendQueryParameter(QUERY_PARAMETER_BATTERY_LEVEL,
                            String.valueOf(batteryLevel))
                    .appendQueryParameter(QUERY_PARAMETER_TIMESTAMP,
                            String.valueOf(System.currentTimeMillis()))
                    .build();

            final String[] columns = new String[] {BATTERY_ESTIMATE, ESTIMATE_READY};
            final Cursor cursor =
                    mContext.getContentResolver().query(contentUri, columns, null, null, null);
            if (cursor == null) {
                Log.w(TAG, "showBatteryPredictionIfNecessary() cursor is null!");
                return;
            }
            try {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    final int estimateReady =
                            cursor.getInt(cursor.getColumnIndex(ESTIMATE_READY));
                    final long batteryEstimate =
                            cursor.getLong(cursor.getColumnIndex(BATTERY_ESTIMATE));
                    if (DEBUG) {
                        Log.d(TAG, "showBatteryTimeIfNecessary() batteryId : " + batteryId
                                + ", ESTIMATE_READY : " + estimateReady
                                + ", BATTERY_ESTIMATE : " + batteryEstimate);
                    }

                    showBatteryPredictionIfNecessary(estimateReady, batteryEstimate, linearLayout);
                    if (batteryId == LEFT_DEVICE_ID) {
                        mIsLeftDeviceEstimateReady = estimateReady == 1;
                    } else if (batteryId == RIGHT_DEVICE_ID) {
                        mIsRightDeviceEstimateReady = estimateReady == 1;
                    }
                }
            } finally {
                cursor.close();
            }
        });
    }

    @VisibleForTesting
    void showBatteryPredictionIfNecessary(int estimateReady, long batteryEstimate,
            LinearLayout linearLayout) {
        ThreadUtils.postOnMainThread(() -> {
            final TextView textView = linearLayout.findViewById(R.id.bt_battery_prediction);
            if (estimateReady == 1) {
                textView.setText(
                        StringUtil.formatElapsedTime(
                                mContext,
                                batteryEstimate,
                                /* withSeconds */ false,
                                /* collapseTimeUnit */  false));
            } else {
                textView.setVisibility(View.GONE);
            }
        });
    }

    @VisibleForTesting
    void showBothDevicesBatteryPredictionIfNecessary() {
        TextView leftDeviceTextView =
                mLayoutPreference.findViewById(R.id.layout_left)
                        .findViewById(R.id.bt_battery_prediction);
        TextView rightDeviceTextView =
                mLayoutPreference.findViewById(R.id.layout_right)
                        .findViewById(R.id.bt_battery_prediction);

        boolean isBothDevicesEstimateReady =
                mIsLeftDeviceEstimateReady && mIsRightDeviceEstimateReady;
        int visibility = isBothDevicesEstimateReady ? View.VISIBLE : View.GONE;
        ThreadUtils.postOnMainThread(() -> {
            leftDeviceTextView.setVisibility(visibility);
            rightDeviceTextView.setVisibility(visibility);
        });
    }

    private void showBatteryIcon(LinearLayout linearLayout, int level, int lowBatteryLevel,
            boolean charging) {
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
        if (DEBUG) {
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
            imageView.setAlpha(1f);
            imageView.setImageBitmap(mIconCache.get(iconUri));
            return;
        }

        imageView.setAlpha(HALF_ALPHA);
        ThreadUtils.postOnBackgroundThread(() -> {
            final Uri uri = Uri.parse(iconUri);
            try {
                mContext.getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);

                final Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        mContext.getContentResolver(), uri);
                ThreadUtils.postOnMainThread(() -> {
                    mIconCache.put(iconUri, bitmap);
                    imageView.setAlpha(1f);
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
