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

import android.app.ActionBar;
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * Detail page for configuring Wi-Fi network.
 *
 * The WifiEntry should be saved to the argument when launching this class in order to properly
 * render this page.
 */
public class ConfigureWifiEntryFragment extends InstrumentedFragment implements WifiConfigUiBase2 {

    private static final String TAG = "ConfigureWifiEntryFragment";

    public static final String NETWORK_CONFIG_KEY = "network_config_key";

    private static final int SUBMIT_BUTTON_ID = android.R.id.button1;
    private static final int CANCEL_BUTTON_ID = android.R.id.button2;

    // Max age of tracked WifiEntries
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating SavedNetworkTracker scans
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    private WifiConfigController2 mUiController;
    private Button mSubmitBtn;
    private Button mCancelBtn;
    private WifiEntry mWifiEntry;
    @VisibleForTesting
    NetworkDetailsTracker mNetworkDetailsTracker;
    private HandlerThread mWorkerThread;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        setupNetworkDetailsTracker();
        mWifiEntry = mNetworkDetailsTracker.getWifiEntry();
    }

    @Override
    public void onDestroy() {
        if (mWorkerThread != null) {
            mWorkerThread.quit();
        }

        super.onDestroy();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_CONFIGURE_NETWORK;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.wifi_add_network_view,
                container, false /* attachToRoot */);

        final Button neutral = rootView.findViewById(android.R.id.button3);
        if (neutral != null) {
            neutral.setVisibility(View.GONE);
        }

        mSubmitBtn = rootView.findViewById(SUBMIT_BUTTON_ID);
        mCancelBtn = rootView.findViewById(CANCEL_BUTTON_ID);
        mSubmitBtn.setOnClickListener(view -> handleSubmitAction());
        mCancelBtn.setOnClickListener(view -> handleCancelAction());

        mUiController = new WifiConfigController2(this, rootView, mWifiEntry, getMode());

        /**
         * For this add WifiEntry UI, need to remove the Home button, so set related feature as
         * false.
         */
        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
        }

        // Resize the layout when keyboard opens.
        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mUiController.showSecurityFields(
            /* refreshEapMethods */ false, /* refreshCertificates */ true);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mUiController.updatePassword();
    }

    @Override
    public int getMode() {
        return WifiConfigUiBase2.MODE_CONNECT;
    }

    @Override
    public WifiConfigController2 getController() {
        return mUiController;
    }

    @Override
    public void dispatchSubmit() {
        handleSubmitAction();
    }

    @Override
    public void setTitle(int id) {
        getActivity().setTitle(id);
    }

    @Override
    public void setTitle(CharSequence title) {
        getActivity().setTitle(title);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        mSubmitBtn.setText(text);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        mCancelBtn.setText(text);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        // AddNetwork doesn't need forget button.
    }

    @Override
    public Button getSubmitButton() {
        return mSubmitBtn;
    }

    @Override
    public Button getCancelButton() {
        return mCancelBtn;
    }

    @Override
    public Button getForgetButton() {
        // AddNetwork doesn't need forget button.
        return null;
    }

    @VisibleForTesting
    void handleSubmitAction() {
        final Intent intent = new Intent();
        final Activity activity = getActivity();
        intent.putExtra(NETWORK_CONFIG_KEY, mUiController.getConfig());
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }

    @VisibleForTesting
    void handleCancelAction() {
        getActivity().finish();
    }

    private void setupNetworkDetailsTracker() {
        if (mNetworkDetailsTracker != null) {
            return;
        }

        final Context context = getContext();
        mWorkerThread = new HandlerThread(TAG
                + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };

        mNetworkDetailsTracker = FeatureFactory.getFeatureFactory()
                .getWifiTrackerLibProvider()
                .createNetworkDetailsTracker(
                        getSettingsLifecycle(),
                        context,
                        new Handler(Looper.getMainLooper()),
                        mWorkerThread.getThreadHandler(),
                        elapsedRealtimeClock,
                        MAX_SCAN_AGE_MILLIS,
                        SCAN_INTERVAL_MILLIS,
                        getArguments().getString(
                                WifiNetworkDetailsFragment.KEY_CHOSEN_WIFIENTRY_KEY));
    }
}
