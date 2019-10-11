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

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.widget.LayoutPreference;

/**
 * This class adds a header with device name.
 */
public class UsbDetailsHeaderController extends UsbDetailsController {
    private static final String KEY_DEVICE_HEADER = "usb_device_header";

    private EntityHeaderController mHeaderController;

    public UsbDetailsHeaderController(Context context, UsbDetailsFragment fragment,
            UsbBackend backend) {
        super(context, fragment, backend);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference headerPreference = screen.findPreference(KEY_DEVICE_HEADER);
        mHeaderController = EntityHeaderController.newInstance(mFragment.getActivity(), mFragment,
                headerPreference.findViewById(R.id.entity_header));
    }


    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        mHeaderController.setLabel(mContext.getString(R.string.usb_pref));
        mHeaderController.setIcon(mContext.getDrawable(R.drawable.ic_usb));
        mHeaderController.done(mFragment.getActivity(), true /* rebindActions */);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DEVICE_HEADER;
    }
}
