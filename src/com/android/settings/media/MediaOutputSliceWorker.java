/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.media;

import static android.media.MediaRoute2ProviderService.REASON_INVALID_COMMAND;
import static android.media.MediaRoute2ProviderService.REASON_NETWORK_ERROR;
import static android.media.MediaRoute2ProviderService.REASON_REJECTED;
import static android.media.MediaRoute2ProviderService.REASON_ROUTE_NOT_AVAILABLE;
import static android.media.MediaRoute2ProviderService.REASON_UNKNOWN_ERROR;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.util.Log;

import com.android.settings.core.instrumentation.SettingsStatsLog;
import com.android.settingslib.media.MediaDevice;

/**
 * SliceBackgroundWorker for the MediaOutputSlice class.
 * It inherits from MediaDeviceUpdateWorker and add metrics logging.
 */
public class MediaOutputSliceWorker extends MediaDeviceUpdateWorker {

    private static final String TAG = "MediaOutputSliceWorker";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    private MediaDevice mSourceDevice, mTargetDevice;
    private int mWiredDeviceCount;
    private int mConnectedBluetoothDeviceCount;
    private int mRemoteDeviceCount;
    private int mAppliedDeviceCountWithinRemoteGroup;

    public MediaOutputSliceWorker(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    public void connectDevice(MediaDevice device) {
        mSourceDevice = mLocalMediaManager.getCurrentConnectedDevice();
        mTargetDevice = device;

        if (DBG) {
            Log.d(TAG, "connectDevice -"
                    + " source:" + mSourceDevice.toString()
                    + " target:" + mTargetDevice.toString());
        }

        super.connectDevice(device);
    }

    private int getLoggingDeviceType(MediaDevice device, boolean isSourceDevice) {
        switch (device.getDeviceType()) {
            case MediaDevice.MediaDeviceType.TYPE_PHONE_DEVICE:
                return isSourceDevice
                        ? SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__BUILTIN_SPEAKER
                        : SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__BUILTIN_SPEAKER;
            case MediaDevice.MediaDeviceType.TYPE_3POINT5_MM_AUDIO_DEVICE:
                return isSourceDevice
                        ? SettingsStatsLog
                        .MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__WIRED_3POINT5_MM_AUDIO
                        : SettingsStatsLog
                                .MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__WIRED_3POINT5_MM_AUDIO;
            case MediaDevice.MediaDeviceType.TYPE_USB_C_AUDIO_DEVICE:
                return isSourceDevice
                        ? SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__USB_C_AUDIO
                        : SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__USB_C_AUDIO;
            case MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE:
                return isSourceDevice
                        ? SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__BLUETOOTH
                        : SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__BLUETOOTH;
            case MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE:
                return isSourceDevice
                        ? SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__REMOTE_SINGLE
                        : SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__REMOTE_SINGLE;
            case MediaDevice.MediaDeviceType.TYPE_CAST_GROUP_DEVICE:
                return isSourceDevice
                        ? SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__REMOTE_GROUP
                        : SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__REMOTE_GROUP;
            default:
                return isSourceDevice
                        ? SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__UNKNOWN_TYPE
                        : SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__UNKNOWN_TYPE;
        }
    }

    private int getLoggingSwitchOpSubResult(int reason) {
        switch (reason) {
            case REASON_REJECTED:
                return SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__REJECTED;
            case REASON_NETWORK_ERROR:
                return SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__NETWORK_ERROR;
            case REASON_ROUTE_NOT_AVAILABLE:
                return SettingsStatsLog
                        .MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__ROUTE_NOT_AVAILABLE;
            case REASON_INVALID_COMMAND:
                return SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__INVALID_COMMAND;
            case REASON_UNKNOWN_ERROR:
            default:
                return SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__UNKNOWN_ERROR;
        }
    }

    private String getLoggingPackageName() {
        final String packageName = getPackageName();
        if (packageName != null && !packageName.isEmpty()) {
            try {
                final ApplicationInfo applicationInfo = mContext.getPackageManager()
                        .getApplicationInfo(packageName, /* default flag */ 0);
                if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    return packageName;
                }
            } catch (Exception ex) {
                Log.e(TAG, packageName + "is invalid.");
            }
        }

        return "";
    }

    private void updateLoggingDeviceCount() {
        mWiredDeviceCount = mConnectedBluetoothDeviceCount = mRemoteDeviceCount = 0;
        mAppliedDeviceCountWithinRemoteGroup = 0;

        for (MediaDevice mediaDevice : mMediaDevices) {
            if (mediaDevice.isConnected()) {
                switch (mediaDevice.getDeviceType()) {
                    case MediaDevice.MediaDeviceType.TYPE_3POINT5_MM_AUDIO_DEVICE:
                    case MediaDevice.MediaDeviceType.TYPE_USB_C_AUDIO_DEVICE:
                        mWiredDeviceCount++;
                        break;
                    case MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE:
                        mConnectedBluetoothDeviceCount++;
                        break;
                    case MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE:
                    case MediaDevice.MediaDeviceType.TYPE_CAST_GROUP_DEVICE:
                        mRemoteDeviceCount++;
                        break;
                    default:
                }
            }
        }

        if (DBG) {
            Log.d(TAG, "connected devices:" + " wired: " + mWiredDeviceCount
                    + " bluetooth: " + mConnectedBluetoothDeviceCount
                    + " remote: " + mRemoteDeviceCount);
        }
    }

    @Override
    public void onSelectedDeviceStateChanged(MediaDevice device, int state) {
        if (DBG) {
            Log.d(TAG, "onSelectedDeviceStateChanged - " + device.toString());
        }

        updateLoggingDeviceCount();

        SettingsStatsLog.write(
                SettingsStatsLog.MEDIAOUTPUT_OP_SWITCH_REPORTED,
                getLoggingDeviceType(mSourceDevice, true),
                getLoggingDeviceType(mTargetDevice, false),
                SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__RESULT__OK,
                SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__NO_ERROR,
                getLoggingPackageName(),
                mWiredDeviceCount,
                mConnectedBluetoothDeviceCount,
                mRemoteDeviceCount,
                mAppliedDeviceCountWithinRemoteGroup);

        super.onSelectedDeviceStateChanged(device, state);
    }

    @Override
    public void onRequestFailed(int reason) {
        if (DBG) {
            Log.e(TAG, "onRequestFailed - " + reason);
        }

        updateLoggingDeviceCount();

        SettingsStatsLog.write(
                SettingsStatsLog.MEDIAOUTPUT_OP_SWITCH_REPORTED,
                getLoggingDeviceType(mSourceDevice, true),
                getLoggingDeviceType(mTargetDevice, false),
                SettingsStatsLog.MEDIA_OUTPUT_OP_SWITCH_REPORTED__RESULT__ERROR,
                getLoggingSwitchOpSubResult(reason),
                getLoggingPackageName(),
                mWiredDeviceCount,
                mConnectedBluetoothDeviceCount,
                mRemoteDeviceCount,
                mAppliedDeviceCountWithinRemoteGroup);

        super.onRequestFailed(reason);
    }
}
