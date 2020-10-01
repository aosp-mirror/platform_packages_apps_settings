/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

/**
 * An AP preference for the currently connected AP.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in {@link ConnectedWifiEntryPreference}.
 */
public class ConnectedAccessPointPreference extends LongPressAccessPointPreference implements
        View.OnClickListener {

    private OnGearClickListener mOnGearClickListener;
    private boolean mIsCaptivePortal;

    public ConnectedAccessPointPreference(AccessPoint accessPoint, Context context,
            UserBadgeCache cache, @DrawableRes int iconResId, boolean forSavedNetworks,
            Fragment fragment) {
        super(accessPoint, context, cache, forSavedNetworks, iconResId, fragment);
    }

    @Override
    protected int getWidgetLayoutResourceId() {
        return R.layout.preference_widget_gear_optional_background;
    }

    @Override
    public void refresh() {
        super.refresh();

        setShowDivider(mIsCaptivePortal);
        if (mIsCaptivePortal) {
            setSummary(R.string.wifi_tap_to_sign_in);
        }
    }

    public void setOnGearClickListener(OnGearClickListener l) {
        mOnGearClickListener = l;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final View gear = holder.findViewById(R.id.settings_button);
        gear.setOnClickListener(this);

        final View gearNoBg = holder.findViewById(R.id.settings_button_no_background);
        gearNoBg.setVisibility(mIsCaptivePortal ? View.INVISIBLE : View.VISIBLE);
        gear.setVisibility(mIsCaptivePortal ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.settings_button) {
            if (mOnGearClickListener != null) {
                mOnGearClickListener.onGearClick(this);
            }
        }
    }

    public void setCaptivePortal(boolean isCaptivePortal) {
        if (mIsCaptivePortal != isCaptivePortal) {
            mIsCaptivePortal = isCaptivePortal;
            refresh();
        }
    }

    public interface OnGearClickListener {
        void onGearClick(ConnectedAccessPointPreference p);
    }

}
