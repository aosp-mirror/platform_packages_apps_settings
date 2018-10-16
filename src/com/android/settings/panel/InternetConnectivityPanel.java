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

package com.android.settings.panel;

import androidx.annotation.VisibleForTesting;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.SettingsSlicesContract;

import com.android.settings.R;
import com.android.settings.wifi.WifiSlice;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Internet Connectivity Panel.
 *
 * <p>
 *     Displays Wifi (full Slice) and Airplane mode.
 * </p>
 */
public class InternetConnectivityPanel implements PanelContent {

    @VisibleForTesting
    static final Uri AIRPLANE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(SettingsSlicesContract.KEY_AIRPLANE_MODE)
            .build();

    private final Context mContext;

    public static InternetConnectivityPanel create(Context context) {
        return new InternetConnectivityPanel(context);
    }

    private InternetConnectivityPanel(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public String getTitle() {
        return (String) mContext.getText(R.string.internet_connectivity_panel_title);
    }

    @Override
    public List<Uri> getSlices() {
        final List<Uri> uris = new ArrayList<>();
        uris.add(WifiSlice.WIFI_URI);
        uris.add(AIRPLANE_URI);
        return uris;
    }

    @Override
    public Intent getSeeMoreIntent() {
        return null;
    }
}
