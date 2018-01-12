/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.hardware.usb;

import android.annotation.SystemService;
import android.content.Context;
import android.hardware.usb.gadget.V1_0.GadgetFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Definitions that were added to UsbManager in P.
 *
 * Copied partially from frameworks/base/core/java/android/hardware/usb/UsbManager to
 * fix issues with roboelectric during test.
 */
@SystemService(Context.USB_SERVICE)
public class UsbManagerExtras {
    public static final long NONE = 0;
    public static final long MTP = GadgetFunction.MTP;
    public static final long PTP = GadgetFunction.PTP;
    public static final long RNDIS = GadgetFunction.RNDIS;
    public static final long MIDI = GadgetFunction.MIDI;
    public static final long ACCESSORY = GadgetFunction.ACCESSORY;
    public static final long AUDIO_SOURCE = GadgetFunction.AUDIO_SOURCE;
    public static final long ADB = GadgetFunction.ADB;

    private static final long SETTABLE_FUNCTIONS = MTP | PTP | RNDIS | MIDI;

    private static final Map<String, Long> STR_MAP = new HashMap<>();

    static {
        STR_MAP.put(UsbManager.USB_FUNCTION_MTP, MTP);
        STR_MAP.put(UsbManager.USB_FUNCTION_PTP, PTP);
        STR_MAP.put(UsbManager.USB_FUNCTION_RNDIS, RNDIS);
        STR_MAP.put(UsbManager.USB_FUNCTION_MIDI, MIDI);
        STR_MAP.put(UsbManager.USB_FUNCTION_ACCESSORY, ACCESSORY);
        STR_MAP.put(UsbManager.USB_FUNCTION_AUDIO_SOURCE, AUDIO_SOURCE);
        STR_MAP.put(UsbManager.USB_FUNCTION_ADB, ADB);
    }

    /**
     * Returns whether the given functions are valid inputs to UsbManager.
     * Currently the empty functions or any of MTP, PTP, RNDIS, MIDI are accepted.
     */
    public static boolean isSettableFunctions(long functions) {
        return (~SETTABLE_FUNCTIONS & functions) == 0;
    }

    /**
     * Returns the string representation of the given functions.
     */
    public static String usbFunctionsToString(long functions) {
        StringJoiner joiner = new StringJoiner(",");
        if ((functions | MTP) != 0) {
            joiner.add(UsbManager.USB_FUNCTION_MTP);
        }
        if ((functions | PTP) != 0) {
            joiner.add(UsbManager.USB_FUNCTION_PTP);
        }
        if ((functions | RNDIS) != 0) {
            joiner.add(UsbManager.USB_FUNCTION_RNDIS);
        }
        if ((functions | MIDI) != 0) {
            joiner.add(UsbManager.USB_FUNCTION_MIDI);
        }
        if ((functions | ACCESSORY) != 0) {
            joiner.add(UsbManager.USB_FUNCTION_ACCESSORY);
        }
        if ((functions | AUDIO_SOURCE) != 0) {
            joiner.add(UsbManager.USB_FUNCTION_AUDIO_SOURCE);
        }
        if ((functions | ADB) != 0) {
            joiner.add(UsbManager.USB_FUNCTION_ADB);
        }
        return joiner.toString();
    }

    /**
     * Parses a string of usb functions and returns a mask of the same functions.
     */
    public static long usbFunctionsFromString(String functions) {
        if (functions == null) {
            return 0;
        }
        long ret = 0;
        for (String function : functions.split(",")) {
            if (STR_MAP.containsKey(function)) {
                ret |= STR_MAP.get(function);
            }
        }
        return ret;
    }
}
