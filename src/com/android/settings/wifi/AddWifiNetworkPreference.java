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
 * limitations under the License
 */

package com.android.settings.wifi;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.wifi.WifiEnterpriseRestrictionUtils;

/**
 * The Preference for users to add Wi-Fi networks in WifiSettings
 */
public class AddWifiNetworkPreference extends RestrictedPreference {

    private static final String TAG = "AddWifiNetworkPreference";

    private final Drawable mScanIconDrawable;

    public AddWifiNetworkPreference(Context context) {
        this(context, null);
    }

    public AddWifiNetworkPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(com.android.settingslib.R.layout.preference_access_point);
        setWidgetLayoutResource(R.layout.wifi_button_preference_widget);
        setIcon(R.drawable.ic_add_24dp);
        setTitle(R.string.wifi_add_network);

        mScanIconDrawable = getDrawable(R.drawable.ic_scan_24dp);
        checkRestrictionAndSetDisabled();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final ImageButton scanButton = (ImageButton) holder.findViewById(R.id.button_icon);
        scanButton.setImageDrawable(mScanIconDrawable);
        scanButton.setContentDescription(
                getContext().getString(R.string.wifi_dpp_scan_qr_code));
        scanButton.setOnClickListener(view -> {
            getContext().startActivity(
                    WifiDppUtils.getEnrolleeQrCodeScannerIntent(getContext(), /* ssid */ null));
        });
    }

    private Drawable getDrawable(@DrawableRes int iconResId) {
        Drawable buttonIcon = null;

        try {
            buttonIcon = getContext().getDrawable(iconResId);
        } catch (Resources.NotFoundException exception) {
            Log.e(TAG, "Resource does not exist: " + iconResId);
        }
        return buttonIcon;
    }

    @VisibleForTesting
    void checkRestrictionAndSetDisabled() {
        checkRestrictionAndSetDisabled(UserManager.DISALLOW_ADD_WIFI_CONFIG);
        if (isDisabledByAdmin()) {
            return;
        }
        if (!WifiEnterpriseRestrictionUtils.isAddWifiConfigAllowed(getContext())) {
            setEnabled(false);
        }
    }
}
