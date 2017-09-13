/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.AlertDialog;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.VisibleForTesting;
import android.util.Pair;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.Utils.ErrorListener;
import com.android.settingslib.graph.BluetoothDeviceLayerDrawable;

import java.util.List;

/**
 * Utils is a helper class that contains constants for various
 * Android resource IDs, debug logging flags, and static methods
 * for creating dialogs.
 */
public final class Utils {
    static final boolean V = com.android.settingslib.bluetooth.Utils.V; // verbose logging
    static final boolean D =  com.android.settingslib.bluetooth.Utils.D;  // regular logging

    private Utils() {
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
        case BluetoothProfile.STATE_CONNECTED:
            return R.string.bluetooth_connected;
        case BluetoothProfile.STATE_CONNECTING:
            return R.string.bluetooth_connecting;
        case BluetoothProfile.STATE_DISCONNECTED:
            return R.string.bluetooth_disconnected;
        case BluetoothProfile.STATE_DISCONNECTING:
            return R.string.bluetooth_disconnecting;
        default:
            return 0;
        }
    }

    // Create (or recycle existing) and show disconnect dialog.
    static AlertDialog showDisconnectDialog(Context context,
            AlertDialog dialog,
            DialogInterface.OnClickListener disconnectListener,
            CharSequence title, CharSequence message) {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context)
                    .setPositiveButton(android.R.string.ok, disconnectListener)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            // use disconnectListener for the correct profile(s)
            CharSequence okText = context.getText(android.R.string.ok);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    okText, disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    // TODO: wire this up to show connection errors...
    static void showConnectingError(Context context, String name) {
        showConnectingError(context, name, getLocalBtManager(context));
    }

    @VisibleForTesting
    static void showConnectingError(Context context, String name, LocalBluetoothManager manager) {
        FeatureFactory.getFactory(context).getMetricsFeatureProvider().visible(context,
            MetricsEvent.VIEW_UNKNOWN, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_CONNECT_ERROR);
        showError(context, name, R.string.bluetooth_connecting_error_message, manager);
    }

    static void showError(Context context, String name, int messageResId) {
        showError(context, name, messageResId, getLocalBtManager(context));
    }

    private static void showError(Context context, String name, int messageResId,
            LocalBluetoothManager manager) {
        String message = context.getString(messageResId, name);
        Context activity = manager.getForegroundActivity();
        if (manager.isForegroundActivity()) {
            new AlertDialog.Builder(activity)
                .setTitle(R.string.bluetooth_error_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return LocalBluetoothManager.getInstance(context, mOnInitCallback);
    }

    public static String createRemoteName(Context context, BluetoothDevice device) {
        String mRemoteName = device != null ? device.getAliasName() : null;

        if (mRemoteName == null) {
            mRemoteName = context.getString(R.string.unknown);
        }
        return mRemoteName;
    }

    private static final ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onShowError(Context context, String name, int messageResId) {
            showError(context, name, messageResId);
        }
    };

    private static final BluetoothManagerCallback mOnInitCallback = new BluetoothManagerCallback() {
        @Override
        public void onBluetoothManagerInitialized(Context appContext,
                LocalBluetoothManager bluetoothManager) {
            com.android.settingslib.bluetooth.Utils.setErrorListener(mErrorListener);
        }
    };

    static Pair<Drawable, String> getBtClassDrawableWithDescription(Context context,
            CachedBluetoothDevice cachedDevice) {
        return getBtClassDrawableWithDescription(context, cachedDevice, 1 /* iconScale */);
    }

    static Pair<Drawable, String> getBtClassDrawableWithDescription(Context context,
            CachedBluetoothDevice cachedDevice, float iconScale) {
        BluetoothClass btClass = cachedDevice.getBtClass();
        final int level = cachedDevice.getBatteryLevel();
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case BluetoothClass.Device.Major.COMPUTER:
                    return new Pair<>(getBluetoothDrawable(context, R.drawable.ic_bt_laptop, level,
                            iconScale),
                            context.getString(R.string.bluetooth_talkback_computer));

                case BluetoothClass.Device.Major.PHONE:
                    return new Pair<>(
                            getBluetoothDrawable(context, R.drawable.ic_bt_cellphone, level,
                                    iconScale),
                            context.getString(R.string.bluetooth_talkback_phone));

                case BluetoothClass.Device.Major.PERIPHERAL:
                    return new Pair<>(
                            getBluetoothDrawable(context, HidProfile.getHidClassDrawable(btClass),
                                    level, iconScale),
                            context.getString(R.string.bluetooth_talkback_input_peripheral));

                case BluetoothClass.Device.Major.IMAGING:
                    return new Pair<>(
                            getBluetoothDrawable(context, R.drawable.ic_settings_print, level,
                                    iconScale),
                            context.getString(R.string.bluetooth_talkback_imaging));

                default:
                    // unrecognized device class; continue
            }
        }

        List<LocalBluetoothProfile> profiles = cachedDevice.getProfiles();
        for (LocalBluetoothProfile profile : profiles) {
            int resId = profile.getDrawableResource(btClass);
            if (resId != 0) {
                return new Pair<>(getBluetoothDrawable(context, resId, level, iconScale), null);
            }
        }
        if (btClass != null) {
            if (btClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) {
                return new Pair<>(
                        getBluetoothDrawable(context, R.drawable.ic_bt_headset_hfp, level,
                                iconScale),
                        context.getString(R.string.bluetooth_talkback_headset));
            }
            if (btClass.doesClassMatch(BluetoothClass.PROFILE_A2DP)) {
                return new Pair<>(
                        getBluetoothDrawable(context, R.drawable.ic_bt_headphones_a2dp, level,
                                iconScale),
                        context.getString(R.string.bluetooth_talkback_headphone));
            }
        }
        return new Pair<>(
                getBluetoothDrawable(context, R.drawable.ic_settings_bluetooth, level, iconScale),
                context.getString(R.string.bluetooth_talkback_bluetooth));
    }

    @VisibleForTesting
    static Drawable getBluetoothDrawable(Context context, @DrawableRes int resId,
            int batteryLevel, float iconScale) {
        if (batteryLevel != BluetoothDevice.BATTERY_LEVEL_UNKNOWN) {
            return BluetoothDeviceLayerDrawable.createLayerDrawable(context, resId, batteryLevel,
                    iconScale);
        } else if (resId != 0) {
            return context.getDrawable(resId);
        } else {
            return null;
        }
    }
}
