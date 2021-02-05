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

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.android.settings.network.InternetUpdater.INTERNET_APM;
import static com.android.settings.network.InternetUpdater.INTERNET_APM_NETWORKS;
import static com.android.settings.network.NetworkProviderSettings.ACTION_NETWORK_PROVIDER_SETTINGS;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.network.AirplaneModePreferenceController;
import com.android.settings.network.InternetUpdater;
import com.android.settings.slices.CustomSliceRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Internet Connectivity Panel.
 */
public class InternetConnectivityPanel implements PanelContent, LifecycleObserver,
        InternetUpdater.OnInternetTypeChangedListener {

    private final Context mContext;
    @VisibleForTesting
    boolean mIsProviderModelEnabled;
    private PanelContentCallback mCallback;
    private InternetUpdater mInternetUpdater;
    private @InternetUpdater.InternetType int mInternetType;

    public static InternetConnectivityPanel create(Context context) {
        return new InternetConnectivityPanel(context);
    }

    private InternetConnectivityPanel(Context context) {
        mContext = context.getApplicationContext();
        mIsProviderModelEnabled = Utils.isProviderModelEnabled(mContext);
        mInternetUpdater = new InternetUpdater(context, null /* Lifecycle */, this);
        mInternetType = mInternetUpdater.getInternetType();
    }

    /** @OnLifecycleEvent(ON_RESUME) */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        if (!mIsProviderModelEnabled) {
            return;
        }
        mInternetUpdater.onResume();
    }

    /** @OnLifecycleEvent(ON_PAUSE) */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        if (!mIsProviderModelEnabled) {
            return;
        }
        mInternetUpdater.onPause();
    }

    /**
     * @return a string for the title of the Panel.
     */
    @Override
    public CharSequence getTitle() {
        if (mIsProviderModelEnabled) {
            return mContext.getText(mInternetType == INTERNET_APM_NETWORKS
                    ? R.string.airplane_mode_network_panel_title
                    : R.string.provider_internet_settings);
        }
        return mContext.getText(R.string.internet_connectivity_panel_title);
    }

    /**
     * @return a string for the subtitle of the Panel.
     */
    @Override
    public CharSequence getSubTitle() {
        if (mIsProviderModelEnabled && mInternetType == INTERNET_APM) {
            return mContext.getText(R.string.condition_airplane_title);
        }
        return null;
    }

    @Override
    public List<Uri> getSlices() {
        final List<Uri> uris = new ArrayList<>();
        if (mIsProviderModelEnabled) {
            uris.add(CustomSliceRegistry.PROVIDER_MODEL_SLICE_URI);
            uris.add(CustomSliceRegistry.AIRPLANE_SAFE_NETWORKS_SLICE_URI);
        } else {
            uris.add(CustomSliceRegistry.WIFI_SLICE_URI);
            uris.add(CustomSliceRegistry.MOBILE_DATA_SLICE_URI);
            uris.add(AirplaneModePreferenceController.SLICE_URI);
        }
        return uris;
    }

    @Override
    public Intent getSeeMoreIntent() {
        return new Intent(mIsProviderModelEnabled
                ? ACTION_NETWORK_PROVIDER_SETTINGS : Settings.ACTION_WIRELESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    public boolean isCustomizedButtonUsed() {
        return mIsProviderModelEnabled;
    }

    @Override
    public CharSequence getCustomizedButtonTitle() {
        if (mInternetType == INTERNET_APM) {
            return null;
        }
        return mContext.getText(R.string.settings_button);
    }

    @Override
    public void onClickCustomizedButton() {
        mContext.startActivity(getSeeMoreIntent());
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_INTERNET_CONNECTIVITY;
    }

    @Override
    public void registerCallback(PanelContentCallback callback) {
        mCallback = callback;
    }

    /**
     * Called when internet type is changed.
     *
     * @param internetType the internet type
     */
    public void onInternetTypeChanged(@InternetUpdater.InternetType int internetType) {
        if (internetType == mInternetType) {
            return;
        }

        final boolean changeToApm = (internetType == INTERNET_APM);
        final boolean changeFromApm = (mInternetType == INTERNET_APM);
        final boolean changeWithApmNetworks =
                (internetType == INTERNET_APM_NETWORKS || mInternetType == INTERNET_APM_NETWORKS);
        mInternetType = internetType;

        if (mCallback != null) {
            if (changeToApm) {
                // The internet type is changed to the airplane mode.
                //   Title: Internet
                //   Sub-Title: Airplane mode is on
                //   Settings button: Hide
                mCallback.onHeaderChanged();
                mCallback.onCustomizedButtonStateChanged();
            } else if (changeFromApm) {
                // The internet type is changed from the airplane mode.
                //   Title: Internet
                //   Settings button: Show
                mCallback.onTitleChanged();
                mCallback.onCustomizedButtonStateChanged();
            } else if (changeWithApmNetworks) {
                // The internet type is changed with the airplane mode networks.
                //   Title: Airplane mode networks / Internet
                mCallback.onTitleChanged();
            }
        }
    }
}
