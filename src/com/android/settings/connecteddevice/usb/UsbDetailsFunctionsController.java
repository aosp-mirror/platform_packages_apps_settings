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

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.net.TetheringManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class controls the radio buttons for choosing between different USB functions.
 */
public class UsbDetailsFunctionsController extends UsbDetailsController
        implements SelectorWithWidgetPreference.OnClickListener {

    private static final String TAG = "UsbFunctionsCtrl";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    static final Map<Long, Integer> FUNCTIONS_MAP = new LinkedHashMap<>();

    static {
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_MTP, R.string.usb_use_file_transfers);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_RNDIS, R.string.usb_use_tethering);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_MIDI, R.string.usb_use_MIDI);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_PTP, R.string.usb_use_photo_transfers);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_UVC, R.string.usb_use_uvc_webcam);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_NONE, R.string.usb_use_charging_only);
    }

    private PreferenceCategory mProfilesContainer;
    private TetheringManager mTetheringManager;
    private Handler mHandler;
    @VisibleForTesting
    OnStartTetheringCallback mOnStartTetheringCallback;
    @VisibleForTesting
    long mPreviousFunction;

    public UsbDetailsFunctionsController(Context context, UsbDetailsFragment fragment,
            UsbBackend backend) {
        super(context, fragment, backend);
        mTetheringManager = context.getSystemService(TetheringManager.class);
        mOnStartTetheringCallback = new OnStartTetheringCallback();
        mPreviousFunction = mUsbBackend.getCurrentFunctions();
        mHandler = new Handler(context.getMainLooper());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mProfilesContainer = screen.findPreference(getPreferenceKey());
        refresh(/* connected */ false, /* functions */ mUsbBackend.getDefaultUsbFunctions(),
                /* powerRole */ 0, /* dataRole */ 0);
    }

    /**
     * Gets a switch preference for the particular option, creating it if needed.
     */
    private SelectorWithWidgetPreference getProfilePreference(String key, int titleId) {
        SelectorWithWidgetPreference pref = mProfilesContainer.findPreference(key);
        if (pref == null) {
            pref = new SelectorWithWidgetPreference(mProfilesContainer.getContext());
            pref.setKey(key);
            pref.setTitle(titleId);
            pref.setSingleLineTitle(false);
            pref.setOnClickListener(this);
            mProfilesContainer.addPreference(pref);
        }
        return pref;
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        if (DEBUG) {
            Log.d(TAG, "refresh() connected : " + connected + ", functions : " + functions
                    + ", powerRole : " + powerRole + ", dataRole : " + dataRole);
        }
        if (!connected || dataRole != DATA_ROLE_DEVICE) {
            mProfilesContainer.setEnabled(false);
        } else {
            // Functions are only available in device mode
            mProfilesContainer.setEnabled(true);
        }
        SelectorWithWidgetPreference pref;
        for (long option : FUNCTIONS_MAP.keySet()) {
            int title = FUNCTIONS_MAP.get(option);
            pref = getProfilePreference(UsbBackend.usbFunctionsToString(option), title);
            // Only show supported options
            if (mUsbBackend.areFunctionsSupported(option)) {
                if (isAccessoryMode(functions)) {
                    pref.setChecked(UsbManager.FUNCTION_MTP == option);
                } else if (functions == UsbManager.FUNCTION_NCM) {
                    pref.setChecked(UsbManager.FUNCTION_RNDIS == option);
                } else {
                    pref.setChecked(functions == option);
                }
            } else {
                mProfilesContainer.removePreference(pref);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        requireAuthAndExecute(() -> {
            final long function = UsbBackend.usbFunctionsFromString(preference.getKey());
            final long previousFunction = mUsbBackend.getCurrentFunctions();
            if (DEBUG) {
                Log.d(TAG, "onRadioButtonClicked() function : " + function + ", toString() : "
                        + UsbManager.usbFunctionsToString(function) + ", previousFunction : "
                        + previousFunction + ", toString() : "
                        + UsbManager.usbFunctionsToString(previousFunction));
            }
            if (function != previousFunction && !Utils.isMonkeyRunning()
                    && !isClickEventIgnored(function, previousFunction)) {
                mPreviousFunction = previousFunction;

                //Update the UI in advance to make it looks smooth
                final SelectorWithWidgetPreference prevPref =
                        (SelectorWithWidgetPreference) mProfilesContainer.findPreference(
                                UsbBackend.usbFunctionsToString(mPreviousFunction));
                if (prevPref != null) {
                    prevPref.setChecked(false);
                    preference.setChecked(true);
                }

                if (function == UsbManager.FUNCTION_RNDIS || function == UsbManager.FUNCTION_NCM) {
                    // We need to have entitlement check for usb tethering, so use API in
                    // TetheringManager.
                    mTetheringManager.startTethering(
                            TetheringManager.TETHERING_USB, new HandlerExecutor(mHandler),
                            mOnStartTetheringCallback);
                } else {
                    mUsbBackend.setCurrentFunctions(function);
                }
            }
        });
    }

    private boolean isClickEventIgnored(long function, long previousFunction) {
        return isAccessoryMode(previousFunction) && function == UsbManager.FUNCTION_MTP;
    }

    private boolean isAccessoryMode(long function) {
        return (function & UsbManager.FUNCTION_ACCESSORY) != 0;
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override
    public String getPreferenceKey() {
        return "usb_details_functions";
    }

    @VisibleForTesting
    final class OnStartTetheringCallback implements TetheringManager.StartTetheringCallback {

        @Override
        public void onTetheringFailed(int error) {
            Log.w(TAG, "onTetheringFailed() error : " + error);
            mUsbBackend.setCurrentFunctions(mPreviousFunction);
        }
    }
}
