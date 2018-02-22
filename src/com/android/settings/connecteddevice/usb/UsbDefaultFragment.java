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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixin;

import com.google.android.collect.Lists;

import java.util.List;

/**
 * Provides options for selecting the default USB mode.
 */
public class UsbDefaultFragment extends RadioButtonPickerFragment {
    @VisibleForTesting
    UsbBackend mUsbBackend;

    private static final String[] FUNCTIONS_LIST = {
            UsbManager.USB_FUNCTION_NONE,
            UsbManager.USB_FUNCTION_MTP,
            UsbManager.USB_FUNCTION_RNDIS,
            UsbManager.USB_FUNCTION_MIDI,
            UsbManager.USB_FUNCTION_PTP
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mUsbBackend = new UsbBackend(context);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        FooterPreferenceMixin footer = new FooterPreferenceMixin(this, this.getLifecycle());
        FooterPreference pref = footer.createFooterPreference();
        pref.setTitle(R.string.usb_default_info);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.USB_DEFAULT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.usb_default_fragment;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        List<CandidateInfo> ret = Lists.newArrayList();
        for (final String option : FUNCTIONS_LIST) {
            int newMode = 0;
            final String title;
            final Context context = getContext();
            if (option.equals(UsbManager.USB_FUNCTION_MTP)) {
                newMode = UsbBackend.MODE_DATA_MTP;
                title = context.getString(R.string.usb_use_file_transfers);
            } else if (option.equals(UsbManager.USB_FUNCTION_PTP)) {
                newMode = UsbBackend.MODE_DATA_PTP;
                title = context.getString(R.string.usb_use_photo_transfers);
            } else if (option.equals(UsbManager.USB_FUNCTION_MIDI)) {
                newMode = UsbBackend.MODE_DATA_MIDI;
                title = context.getString(R.string.usb_use_MIDI);
            } else if (option.equals(UsbManager.USB_FUNCTION_RNDIS)) {
                newMode = UsbBackend.MODE_DATA_TETHER;
                title = context.getString(R.string.usb_use_tethering);
            } else if (option.equals(UsbManager.USB_FUNCTION_NONE)) {
                newMode = UsbBackend.MODE_DATA_NONE;
                title = context.getString(R.string.usb_use_charging_only);
            } else {
                title = "";
            }

            // Only show supported and allowed options
            if (mUsbBackend.isModeSupported(newMode)
                    && !mUsbBackend.isModeDisallowedBySystem(newMode)
                    && !mUsbBackend.isModeDisallowed(newMode)) {
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
                        return option;
                    }
                });
            }
        }
        return ret;
    }

    @Override
    protected String getDefaultKey() {
        switch (mUsbBackend.getDefaultUsbMode()) {
            case UsbBackend.MODE_DATA_MTP:
                return UsbManager.USB_FUNCTION_MTP;
            case UsbBackend.MODE_DATA_PTP:
                return UsbManager.USB_FUNCTION_PTP;
            case UsbBackend.MODE_DATA_TETHER:
                return UsbManager.USB_FUNCTION_RNDIS;
            case UsbBackend.MODE_DATA_MIDI:
                return UsbManager.USB_FUNCTION_MIDI;
            default:
                return UsbManager.USB_FUNCTION_NONE;
        }
    }

    @Override
    protected boolean setDefaultKey(String key) {
        int thisMode = UsbBackend.MODE_DATA_NONE;
        if (key.equals(UsbManager.USB_FUNCTION_MTP)) {
            thisMode = UsbBackend.MODE_DATA_MTP;
        } else if (key.equals(UsbManager.USB_FUNCTION_PTP)) {
            thisMode = UsbBackend.MODE_DATA_PTP;
        } else if (key.equals(UsbManager.USB_FUNCTION_RNDIS)) {
            thisMode = UsbBackend.MODE_DATA_TETHER;
        } else if (key.equals(UsbManager.USB_FUNCTION_MIDI)) {
            thisMode = UsbBackend.MODE_DATA_MIDI;
        }
        if (!Utils.isMonkeyRunning()) {
            mUsbBackend.setDefaultUsbMode(thisMode);
        }
        return true;
    }
}