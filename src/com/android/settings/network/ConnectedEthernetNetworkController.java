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

package com.android.settings.network;

import static com.android.settings.network.InternetUpdater.INTERNET_ETHERNET;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * PreferenceController to show the connected ethernet network.
 */
public class ConnectedEthernetNetworkController extends AbstractPreferenceController
        implements InternetUpdater.InternetChangeListener {

    public static final String KEY = "connected_ethernet_network";

    private Preference mPreference;
    private InternetUpdater mInternetUpdater;
    private @InternetUpdater.InternetType int mInternetType;

    public ConnectedEthernetNetworkController(Context context, Lifecycle lifecycle) {
        super(context);
        mInternetUpdater = new InternetUpdater(context, lifecycle, this);
        mInternetType = mInternetUpdater.getInternetType();
    }

    @Override
    public boolean isAvailable() {
        return mInternetType == INTERNET_ETHERNET;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);
        final Drawable drawable = mContext.getDrawable(R.drawable.ic_settings_ethernet);
        if (drawable != null) {
            drawable.setTintList(
                    Utils.getColorAttr(mContext, android.R.attr.colorControlActivated));
            mPreference.setIcon(drawable);
        }
    }

    /**
     * Called when internet type is changed.
     *
     * @param internetType the internet type
     */
    public void onInternetTypeChanged(@InternetUpdater.InternetType int internetType) {
        mInternetType = internetType;
        if (mPreference != null) {
            mPreference.setVisible(isAvailable());
        }
    }
}
