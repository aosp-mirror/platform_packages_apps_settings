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

package com.android.settings.connecteddevice.usb;

import static android.net.TetheringManager.TETHERING_USB;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbManager;
import android.net.TetheringManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import com.google.android.collect.Lists;

import java.util.List;

/**
 * Provides options for selecting the default USB mode.
 */
public class UsbDefaultFragment extends RadioButtonPickerFragment {

    private static final String TAG = "UsbDefaultFragment";

    @VisibleForTesting
    UsbBackend mUsbBackend;
    @VisibleForTesting
    TetheringManager mTetheringManager;
    @VisibleForTesting
    OnStartTetheringCallback mOnStartTetheringCallback = new OnStartTetheringCallback();
    @VisibleForTesting
    long mPreviousFunctions;
    @VisibleForTesting
    long mCurrentFunctions;
    @VisibleForTesting
    boolean mIsStartTethering = false;
    @VisibleForTesting
    Handler mHandler;

    private UsbConnectionBroadcastReceiver mUsbReceiver;
    private boolean mIsConnected = false;

    @VisibleForTesting
    UsbConnectionBroadcastReceiver.UsbConnectionListener mUsbConnectionListener =
            (connected, functions, powerRole, dataRole, isUsbConfigured) -> {
                final long defaultFunctions = mUsbBackend.getDefaultUsbFunctions();
                Log.d(TAG, "UsbConnectionListener() connected : " + connected + ", functions : "
                        + functions + ", defaultFunctions : " + defaultFunctions
                        + ", mIsStartTethering : " + mIsStartTethering
                        + ", isUsbConfigured : " + isUsbConfigured);
                if (connected && !mIsConnected && ((defaultFunctions == UsbManager.FUNCTION_RNDIS
                        || defaultFunctions == UsbManager.FUNCTION_NCM)
                        && defaultFunctions == functions)
                        && !mIsStartTethering) {
                    mCurrentFunctions = defaultFunctions;
                    startTethering();
                }

                if ((mIsStartTethering || isUsbConfigured) && connected) {
                    mCurrentFunctions = functions;
                    refresh(functions);
                    mIsStartTethering = false;
                }
                mIsConnected = connected;
            };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mUsbBackend = new UsbBackend(context);
        mTetheringManager = context.getSystemService(TetheringManager.class);
        mUsbReceiver = new UsbConnectionBroadcastReceiver(context, mUsbConnectionListener,
                mUsbBackend);
        mHandler = new Handler(context.getMainLooper());
        getSettingsLifecycle().addObserver(mUsbReceiver);
        mCurrentFunctions = mUsbBackend.getDefaultUsbFunctions();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        getPreferenceScreen().addPreference(new FooterPreference.Builder(getActivity()).setTitle(
                R.string.usb_default_info).build());
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USB_DEFAULT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.usb_default_fragment;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        List<CandidateInfo> ret = Lists.newArrayList();
        for (final long option : UsbDetailsFunctionsController.FUNCTIONS_MAP.keySet()) {
            final String title = getContext().getString(
                    UsbDetailsFunctionsController.FUNCTIONS_MAP.get(option));
            final String key = UsbBackend.usbFunctionsToString(option);

            // Only show supported functions
            if (mUsbBackend.areFunctionsSupported(option)) {
                ret.add(new CandidateInfo(true /* enabled */) {
                    @Override
                    public CharSequence loadLabel() {
                        return title;
                    }

                    @Override
                    public Drawable loadIcon() {
                        return null;
                    }

                    @Override
                    public String getKey() {
                        return key;
                    }
                });
            }
        }
        return ret;
    }

    @Override
    protected String getDefaultKey() {
        long defaultUsbFunctions = mUsbBackend.getDefaultUsbFunctions();
        // Because we didn't have an option for NCM, so make FUNCTION_NCM corresponding to
        // FUNCTION_RNDIS for initializing the UI.
        return UsbBackend.usbFunctionsToString(defaultUsbFunctions == UsbManager.FUNCTION_NCM
                ? UsbManager.FUNCTION_RNDIS : defaultUsbFunctions);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        long functions = UsbBackend.usbFunctionsFromString(key);
        mPreviousFunctions = mUsbBackend.getCurrentFunctions();
        if (!Utils.isMonkeyRunning()) {
            if (functions == UsbManager.FUNCTION_RNDIS || functions == UsbManager.FUNCTION_NCM) {
                // We need to have entitlement check for usb tethering, so use API in
                // TetheringManager.
                mCurrentFunctions = functions;
                startTethering();
            } else {
                mIsStartTethering = false;
                mCurrentFunctions = functions;
                mUsbBackend.setDefaultUsbFunctions(functions);
            }

        }
        return true;
    }

    private void startTethering() {
        Log.d(TAG, "startTethering()");
        mIsStartTethering = true;
        mTetheringManager.startTethering(TETHERING_USB, new HandlerExecutor(mHandler),
                mOnStartTetheringCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCurrentFunctions = mUsbBackend.getCurrentFunctions();
        Log.d(TAG, "onPause() : current functions : " + mCurrentFunctions);
        mUsbBackend.setDefaultUsbFunctions(mCurrentFunctions);
    }

    @VisibleForTesting
    final class OnStartTetheringCallback implements
            TetheringManager.StartTetheringCallback {

        @Override
        public void onTetheringStarted() {
            // Set default usb functions again to make internal data persistent
            mCurrentFunctions = mUsbBackend.getCurrentFunctions();
            Log.d(TAG, "onTetheringStarted() : mCurrentFunctions " + mCurrentFunctions);
            mUsbBackend.setDefaultUsbFunctions(mCurrentFunctions);
        }

        @Override
        public void onTetheringFailed(int error) {
            Log.w(TAG, "onTetheringFailed() error : " + error);
            mUsbBackend.setDefaultUsbFunctions(mPreviousFunctions);
            updateCandidates();
        }
    }

    private void refresh(long functions) {
        final PreferenceScreen screen = getPreferenceScreen();
        for (long option : UsbDetailsFunctionsController.FUNCTIONS_MAP.keySet()) {
            final SelectorWithWidgetPreference pref =
                    screen.findPreference(UsbBackend.usbFunctionsToString(option));
            if (pref != null) {
                final boolean isSupported = mUsbBackend.areFunctionsSupported(option);
                pref.setEnabled(isSupported);
                if (isSupported) {
                    if (functions == UsbManager.FUNCTION_NCM) {
                        pref.setChecked(UsbManager.FUNCTION_RNDIS == option);
                    } else {
                        pref.setChecked(functions == option);
                    }
                }
            }
        }
    }
}