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

package com.android.settings.connecteddevice.stylus;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import androidx.preference.Preference;

import java.util.List;

import javax.annotation.Nullable;

/** FeatureProvider for USB settings **/
public interface StylusFeatureProvider {

    /**
     * Returns whether the current attached USB device allows firmware updates.
     *
     * @param usbDevice The USB device to check
     */
    boolean isUsbFirmwareUpdateEnabled(UsbDevice usbDevice);

    /**
     * Returns a list of preferences for the connected USB device if exists. If not, returns
     * null. If an update is not available but firmware update feature is enabled for the device,
     * the list will contain only the preference showing the current firmware version.
     *
     * @param context The context
     * @param usbDevice The USB device for which to generate preferences.
     */
    @Nullable
    List<Preference> getUsbFirmwareUpdatePreferences(Context context, UsbDevice usbDevice);
}
