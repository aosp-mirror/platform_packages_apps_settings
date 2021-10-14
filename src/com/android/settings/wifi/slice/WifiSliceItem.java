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

package com.android.settings.wifi.slice;

import android.content.Context;
import android.text.TextUtils;

import com.android.settingslib.R;
import com.android.wifitrackerlib.WifiEntry;

/**
 * The data set which is needed by a Wi-Fi Slice, it collects necessary data from {@link WifiEntry}
 * and provides similar getter methods for corresponding data.
 */
public class WifiSliceItem {

    private final Context mContext;
    private final String mKey;
    private final String mTitle;
    private final int mSecurity;
    private final int mConnectedState;
    private final int mLevel;
    private final boolean mShouldShowXLevelIcon;
    private final boolean mShouldEditBeforeConnect;
    private final boolean mHasInternetAccess;
    private final String mSummary;

    // These values must be kept within [WifiEntry.WIFI_LEVEL_MIN, WifiEntry.WIFI_LEVEL_MAX]
    private static final int[] WIFI_CONNECTION_STRENGTH = {
            R.string.accessibility_no_wifi,
            R.string.accessibility_wifi_one_bar,
            R.string.accessibility_wifi_two_bars,
            R.string.accessibility_wifi_three_bars,
            R.string.accessibility_wifi_signal_full
    };

    public WifiSliceItem(Context context, WifiEntry wifiEntry) {
        mContext = context;
        mKey = wifiEntry.getKey();
        mTitle = wifiEntry.getTitle();
        mSecurity = wifiEntry.getSecurity();
        mConnectedState = wifiEntry.getConnectedState();
        mLevel = wifiEntry.getLevel();
        mShouldShowXLevelIcon = wifiEntry.shouldShowXLevelIcon();
        mShouldEditBeforeConnect = wifiEntry.shouldEditBeforeConnect();
        mHasInternetAccess = wifiEntry.hasInternetAccess();
        mSummary = wifiEntry.getSummary(false /* concise */);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof WifiSliceItem)) {
            return false;
        }

        final WifiSliceItem otherItem = (WifiSliceItem) other;
        if (!TextUtils.equals(getKey(), otherItem.getKey())) {
            return false;
        }
        if (getConnectedState() != otherItem.getConnectedState()) {
            return false;
        }
        if (getLevel() != otherItem.getLevel()) {
            return false;
        }
        if (shouldShowXLevelIcon() != otherItem.shouldShowXLevelIcon()) {
            return false;
        }
        if (!TextUtils.equals(getSummary(), otherItem.getSummary())) {
            return false;
        }
        return true;
    }

    public String getKey() {
        return mKey;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getSecurity() {
        return mSecurity;
    }

    public int getConnectedState() {
        return mConnectedState;
    }

    public int getLevel() {
        return mLevel;
    }

    /**
     * Returns whether the level icon for this network should show an X or not.
     */
    public boolean shouldShowXLevelIcon() {
        return mShouldShowXLevelIcon;
    }

    /**
     * Returns true when the Wi-Fi network has Internet access.
     */
    public boolean hasInternetAccess() {
        return mHasInternetAccess;
    }

    /**
     * In Wi-Fi picker, when users click a saved network, it will connect to the Wi-Fi network.
     * However, for some special cases, Wi-Fi picker should show Wi-Fi editor UI for users to edit
     * security or password before connecting. Or users will always get connection fail results.
     */
    public boolean shouldEditBeforeConnect() {
        return mShouldEditBeforeConnect;
    }

    /**
     * Returns a 'NOT' concise summary, this is different from WifiEntry#getSummary().
     */
    public String getSummary() {
        return mSummary;
    }

    /**
     * This method has similar code as WifiEntryPreference#buildContentDescription().
     * TODO(b/154191825): Adds WifiEntry#getContentDescription() to replace the duplicate code.
     */
    public CharSequence getContentDescription() {
        CharSequence contentDescription = mTitle;
        if (!TextUtils.isEmpty(mSummary)) {
            contentDescription = TextUtils.concat(contentDescription, ",", mSummary);
        }
        if (mLevel >= 0 && mLevel < WIFI_CONNECTION_STRENGTH.length) {
            contentDescription = TextUtils.concat(contentDescription, ",",
                    mContext.getString(WIFI_CONNECTION_STRENGTH[mLevel]));
        }
        return TextUtils.concat(contentDescription, ",", mSecurity == WifiEntry.SECURITY_NONE
                ? mContext.getString(R.string.accessibility_wifi_security_type_none)
                : mContext.getString(R.string.accessibility_wifi_security_type_secured));
    }
}
