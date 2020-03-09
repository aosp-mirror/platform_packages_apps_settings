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
package com.android.settings.wifi;

import android.content.Context;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.R;
import com.android.settingslib.wifi.LongPressWifiEntryPreference;
import com.android.wifitrackerlib.WifiEntry;

/**
 * An AP preference for the currently connected AP.
 */
public class ConnectedWifiEntryPreference extends LongPressWifiEntryPreference implements
        View.OnClickListener {

    private OnGearClickListener mOnGearClickListener;

    public ConnectedWifiEntryPreference(Context context, WifiEntry wifiEntry, Fragment fragment) {
        super(context, wifiEntry, fragment);
        setWidgetLayoutResource(R.layout.preference_widget_gear_optional_background);
    }

    /**
     * Set gear icon click callback listener.
     */
    public void setOnGearClickListener(OnGearClickListener l) {
        mOnGearClickListener = l;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final View gear = holder.findViewById(R.id.settings_button);
        gear.setOnClickListener(this);

        final boolean canSignIn = getWifiEntry().canSignIn();
        holder.findViewById(R.id.settings_button_no_background).setVisibility(
                canSignIn ? View.INVISIBLE : View.VISIBLE);
        gear.setVisibility(canSignIn ? View.VISIBLE : View.INVISIBLE);
        holder.findViewById(R.id.two_target_divider).setVisibility(
                canSignIn ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.settings_button) {
            if (mOnGearClickListener != null) {
                mOnGearClickListener.onGearClick(this);
            }
        }
    }

    /**
     * Gear Icon click callback interface.
     */
    public interface OnGearClickListener {
        /**
         * The callback triggered when gear icon is clicked.
         */
        void onGearClick(ConnectedWifiEntryPreference p);
    }

}
