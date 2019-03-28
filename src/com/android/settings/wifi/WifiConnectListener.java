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

package com.android.settings.wifi;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.android.settings.R;

/**
 *  A listener to display a toast on failure to connect
 */
public class WifiConnectListener implements WifiManager.ActionListener {

    private final Context mContext;

    public WifiConnectListener(Context context) {
        mContext = context;
    }

    @Override
    public void onSuccess() {
    }

    @Override
    public void onFailure(int reason) {
        if (mContext != null) {
            Toast.makeText(mContext,
                    R.string.wifi_failed_connect_message,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
