/*
 * Copyright (C) 2021 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceViewHolder;

import com.android.wifitrackerlib.WifiEntry;

/**
 * WifiEntryPreference that can be long pressed.
 */
public class LongPressWifiEntryPreference extends WifiEntryPreference {

    private final Fragment mFragment;

    public LongPressWifiEntryPreference(Context context, WifiEntry wifiEntry, Fragment fragment) {
        super(context, wifiEntry);
        mFragment = fragment;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mFragment != null) {
            view.itemView.setOnCreateContextMenuListener(mFragment);
            view.itemView.setTag(this);
            view.itemView.setLongClickable(true);
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        setEnabled(shouldEnabled());
    }

    @VisibleForTesting
    boolean shouldEnabled() {
        WifiEntry wifiEntry = getWifiEntry();
        if (wifiEntry == null) return false;

        boolean enabled = wifiEntry.canConnect();
        // If Wi-Fi is connected or saved network, leave it enabled to disconnect or configure.
        if (!enabled && (wifiEntry.canDisconnect() || wifiEntry.isSaved())) {
            enabled = true;
        }
        return enabled;
    }
}
