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
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;

import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the USB device details and provides updates to individual controllers.
 */
public class UsbDetailsFragment extends DashboardFragment {
    private static final String TAG = UsbDetailsFragment.class.getSimpleName();

    private List<UsbDetailsController> mControllers;
    private UsbBackend mUsbBackend;

    @VisibleForTesting
    UsbConnectionBroadcastReceiver mUsbReceiver;

    private UsbConnectionBroadcastReceiver.UsbConnectionListener mUsbConnectionListener =
            (connected, newMode) -> {
                if (!connected) {
                    this.finish();
                } else {
                    for (UsbDetailsController controller : mControllers) {
                        controller.refresh(newMode);
                    }
                }
            };

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.USB_DEVICE_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.usb_details_fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        mUsbBackend = new UsbBackend(context);
        mControllers = createControllerList(context, mUsbBackend, this);
        mUsbReceiver = new UsbConnectionBroadcastReceiver(context, mUsbConnectionListener,
                mUsbBackend);
        this.getLifecycle().addObserver(mUsbReceiver);

        List<AbstractPreferenceController> ret = new ArrayList<>();
        ret.addAll(mControllers);
        return ret;
    }

    private static List<UsbDetailsController> createControllerList(Context context,
            UsbBackend usbBackend, DashboardFragment fragment) {
        List<UsbDetailsController> ret = new ArrayList<>();
        ret.add(new UsbDetailsHeaderController(context, fragment, usbBackend));
        ret.add(new UsbDetailsProfilesController(context, fragment,
                usbBackend, Lists.newArrayList(UsbManager.USB_FUNCTION_MTP), "usb_main_options"));
        ret.add(new UsbDetailsProfilesController(context, fragment,
                usbBackend, Lists.newArrayList(UsbDetailsProfilesController.KEY_POWER,
                UsbManager.USB_FUNCTION_RNDIS, UsbManager.USB_FUNCTION_MIDI,
                UsbManager.USB_FUNCTION_PTP), "usb_secondary_options"));
        return ret;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    return new ArrayList<>();
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return super.getNonIndexableKeys(context);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    List<AbstractPreferenceController> ret = new ArrayList<>();
                    ret.addAll(createControllerList(context, new UsbBackend(context), null));
                    return ret;
                }
            };
}
