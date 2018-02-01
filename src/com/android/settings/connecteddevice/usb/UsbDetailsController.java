/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.support.annotation.UiThread;
import android.support.v14.preference.PreferenceFragment;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * This class provides common members and refresh functionality for usb controllers.
 */
public abstract class UsbDetailsController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    protected final Context mContext;
    protected final PreferenceFragment mFragment;
    protected final UsbBackend mUsbBackend;

    public UsbDetailsController(Context context, PreferenceFragment fragment, UsbBackend backend) {
        super(context);
        mContext = context;
        mFragment = fragment;
        mUsbBackend = backend;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * This method is called when the USB mode has changed and the controller needs to update.
     * @param newMode the new mode, made up of OR'd values from UsbBackend
     */
    @UiThread
    protected abstract void refresh(int newMode);
}
