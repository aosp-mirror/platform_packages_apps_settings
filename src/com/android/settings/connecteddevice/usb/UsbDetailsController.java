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
import android.os.Handler;

import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * This class provides common members and refresh functionality for usb controllers.
 */
public abstract class UsbDetailsController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    protected final Context mContext;
    protected final UsbDetailsFragment mFragment;
    protected final UsbBackend mUsbBackend;

    @VisibleForTesting
    Handler mHandler;

    public UsbDetailsController(Context context, UsbDetailsFragment fragment, UsbBackend backend) {
        super(context);
        mContext = context;
        mFragment = fragment;
        mUsbBackend = backend;
        mHandler = new Handler(context.getMainLooper());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Called when the USB state has changed, so that this component can be refreshed.
     *
     * @param connected Whether USB is connected
     * @param functions A mask of the currently enabled functions
     * @param powerRole The current power role
     * @param dataRole The current data role
     */
    @UiThread
    protected abstract void refresh(boolean connected, long functions, int powerRole, int dataRole);
}
