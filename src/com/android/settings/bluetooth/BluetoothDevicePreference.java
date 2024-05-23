/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BluetoothDevicePreference is the preference type used to display each remote
 * Bluetooth device in the Bluetooth Settings screen.
 */
public final class BluetoothDevicePreference extends GearPreference {
    private static final String TAG = "BluetoothDevicePref";

    private static int sDimAlpha = Integer.MIN_VALUE;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SortType.TYPE_DEFAULT,
            SortType.TYPE_FIFO,
            SortType.TYPE_NO_SORT})
    public @interface SortType {
        int TYPE_DEFAULT = 1;
        int TYPE_FIFO = 2;
        int TYPE_NO_SORT = 3;
    }

    private final CachedBluetoothDevice mCachedDevice;
    private final UserManager mUserManager;

    private Set<BluetoothDevice> mBluetoothDevices;
    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;
    private final boolean mShowDevicesWithoutNames;
    @NonNull
    private static final AtomicInteger sNextId = new AtomicInteger();
    private final int mId;
    private final int mType;

    private AlertDialog mDisconnectDialog;
    private String contentDescription = null;
    private boolean mHideSecondTarget = false;
    private boolean mIsCallbackRemoved = true;
    @VisibleForTesting
    boolean mNeedNotifyHierarchyChanged = false;
    /* Talk-back descriptions for various BT icons */
    Resources mResources;
    final BluetoothDevicePreferenceCallback mCallback;
    @VisibleForTesting
    final BluetoothAdapter.OnMetadataChangedListener mMetadataListener =
            new BluetoothAdapter.OnMetadataChangedListener() {
                @Override
                public void onMetadataChanged(BluetoothDevice device, int key, byte[] value) {
                    Log.d(TAG, String.format("Metadata updated in Device %s: %d = %s.",
                            device.getAnonymizedAddress(),
                            key, value == null ? null : new String(value)));
                    onPreferenceAttributesChanged();
                }
            };

    private class BluetoothDevicePreferenceCallback implements CachedBluetoothDevice.Callback {

        @Override
        public void onDeviceAttributesChanged() {
            onPreferenceAttributesChanged();
        }
    }

    public BluetoothDevicePreference(Context context, CachedBluetoothDevice cachedDevice,
            boolean showDeviceWithoutNames, @SortType int type) {
        super(context, null);
        mResources = getContext().getResources();
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mShowDevicesWithoutNames = showDeviceWithoutNames;

        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
            sDimAlpha = (int) (outValue.getFloat() * 255);
        }

        mCachedDevice = cachedDevice;
        mCallback = new BluetoothDevicePreferenceCallback();
        mId = sNextId.getAndIncrement();
        mType = type;
        setVisible(false);

        onPreferenceAttributesChanged();
    }

    public void setNeedNotifyHierarchyChanged(boolean needNotifyHierarchyChanged) {
        mNeedNotifyHierarchyChanged = needNotifyHierarchyChanged;
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return mCachedDevice == null
                || mCachedDevice.getBondState() != BluetoothDevice.BOND_BONDED
                || mUserManager.hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH)
                || mHideSecondTarget;
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_gear;
    }

    public CachedBluetoothDevice getCachedDevice() {
        return mCachedDevice;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        if (!mIsCallbackRemoved) {
            mCachedDevice.unregisterCallback(mCallback);
            unregisterMetadataChangedListener();
            mIsCallbackRemoved = true;
        }
        if (mDisconnectDialog != null) {
            mDisconnectDialog.dismiss();
            mDisconnectDialog = null;
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (mIsCallbackRemoved) {
            mCachedDevice.registerCallback(mCallback);
            registerMetadataChangedListener();
            mIsCallbackRemoved = false;
        }
        onPreferenceAttributesChanged();
    }

    @Override
    public void onDetached() {
        super.onDetached();
        if (!mIsCallbackRemoved) {
            mCachedDevice.unregisterCallback(mCallback);
            unregisterMetadataChangedListener();
            mIsCallbackRemoved = true;
        }
    }

    private void registerMetadataChangedListener() {
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
        for (CachedBluetoothDevice cbd : mCachedDevice.getMemberDevice()) {
            mBluetoothDevices.add(cbd.getDevice());
        }
        if (mBluetoothDevices.isEmpty()) {
            Log.d(TAG, "No BT device to register.");
            return;
        }
        Set<BluetoothDevice> errorDevices = new HashSet<>();
        mBluetoothDevices.forEach(bd -> {
            try {
                boolean isSuccess = mBluetoothAdapter.addOnMetadataChangedListener(bd,
                        getContext().getMainExecutor(), mMetadataListener);
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

    private void unregisterMetadataChangedListener() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "No mBluetoothAdapter");
            return;
        }
        if (mBluetoothDevices == null || mBluetoothDevices.isEmpty()) {
            Log.d(TAG, "No BT device to unregister.");
            return;
        }
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

    public CachedBluetoothDevice getBluetoothDevice() {
        return mCachedDevice;
    }

    public void hideSecondTarget(boolean hideSecondTarget) {
        mHideSecondTarget = hideSecondTarget;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void onPreferenceAttributesChanged() {
        try {
            ThreadUtils.postOnBackgroundThread(() -> {
                @Nullable String name = mCachedDevice.getName();
                // Null check is done at the framework
                @Nullable String connectionSummary = getConnectionSummary();
                @NonNull Pair<Drawable, String> pair = mCachedDevice.getDrawableWithDescription();
                boolean isBusy = mCachedDevice.isBusy();
                // Device is only visible in the UI if it has a valid name besides MAC address or
                // when user allows showing devices without user-friendly name in developer settings
                boolean isVisible =
                        mShowDevicesWithoutNames || mCachedDevice.hasHumanReadableName();

                ThreadUtils.postOnMainThread(() -> {
                    /*
                     * The preference framework takes care of making sure the value has
                     * changed before proceeding. It will also call notifyChanged() if
                     * any preference info has changed from the previous value.
                     */
                    setTitle(name);
                    setSummary(connectionSummary);
                    setIcon(pair.first);
                    contentDescription = pair.second;
                    // Used to gray out the item
                    setEnabled(!isBusy);
                    setVisible(isVisible);

                    // This could affect ordering, so notify that
                    if (mNeedNotifyHierarchyChanged) {
                        notifyHierarchyChanged();
                    }
                });
            });
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "Handler thread unavailable, skipping getConnectionSummary!");
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        // Disable this view if the bluetooth enable/disable preference view is off
        if (null != findPreferenceInHierarchy("bt_checkbox")) {
            setDependency("bt_checkbox");
        }

        if (mCachedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            ImageView deviceDetails = (ImageView) view.findViewById(R.id.settings_button);

            if (deviceDetails != null) {
                deviceDetails.setOnClickListener(this);
            }
        }
        final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        if (imageView != null) {
            imageView.setContentDescription(contentDescription);
            // Set property to prevent Talkback from reading out.
            imageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            imageView.setElevation(
                    getContext().getResources().getDimension(R.dimen.bt_icon_elevation));
        }
        super.onBindViewHolder(view);
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof BluetoothDevicePreference)) {
            return false;
        }
        return mCachedDevice.equals(
                ((BluetoothDevicePreference) o).mCachedDevice);
    }

    @Override
    public int hashCode() {
        return mCachedDevice.hashCode();
    }

    @Override
    public int compareTo(Preference another) {
        if (!(another instanceof BluetoothDevicePreference)) {
            // Rely on default sort
            return super.compareTo(another);
        }

        switch (mType) {
            case SortType.TYPE_DEFAULT:
                return mCachedDevice
                        .compareTo(((BluetoothDevicePreference) another).mCachedDevice);
            case SortType.TYPE_FIFO:
                return mId > ((BluetoothDevicePreference) another).mId ? 1 : -1;
            default:
                return super.compareTo(another);
        }
    }

    /**
     * Performs different actions according to the device connected and bonded state after
     * clicking on the preference.
     */
    public void onClicked() {
        Context context = getContext();
        int bondState = mCachedDevice.getBondState();

        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();

        if (mCachedDevice.isConnected()) {
            metricsFeatureProvider.action(context,
                    SettingsEnums.ACTION_SETTINGS_BLUETOOTH_DISCONNECT);
            askDisconnect();
        } else if (bondState == BluetoothDevice.BOND_BONDED) {
            metricsFeatureProvider.action(context,
                    SettingsEnums.ACTION_SETTINGS_BLUETOOTH_CONNECT);
            mCachedDevice.connect();
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            metricsFeatureProvider.action(context,
                    SettingsEnums.ACTION_SETTINGS_BLUETOOTH_PAIR);
            if (!mCachedDevice.hasHumanReadableName()) {
                metricsFeatureProvider.action(context,
                        SettingsEnums.ACTION_SETTINGS_BLUETOOTH_PAIR_DEVICES_WITHOUT_NAMES);
            }
            pair();
        }
    }

    // Show disconnect confirmation dialog for a device.
    private void askDisconnect() {
        Context context = getContext();
        String name = mCachedDevice.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.bluetooth_device);
        }
        String message = context.getString(R.string.bluetooth_disconnect_all_profiles, name);
        String title = context.getString(R.string.bluetooth_disconnect_title);

        DialogInterface.OnClickListener disconnectListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mCachedDevice.disconnect();
            }
        };

        mDisconnectDialog = Utils.showDisconnectDialog(context,
                mDisconnectDialog, disconnectListener, title, Html.fromHtml(message));
    }

    private void pair() {
        if (!mCachedDevice.startPairing()) {
            Utils.showError(getContext(), mCachedDevice.getName(),
                    com.android.settingslib.R.string.bluetooth_pairing_error_message);
        }
    }

    private String getConnectionSummary() {
        String summary = null;
        if (mCachedDevice.getBondState() != BluetoothDevice.BOND_NONE) {
            summary = mCachedDevice.getConnectionSummary();
        }
        return summary;
    }
}
