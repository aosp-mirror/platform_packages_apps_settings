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

package com.android.settings.network.telephony;


import android.telephony.TelephonyManager;

/**
 * Contains hidden constants copied from the platform.
 */
public class TelephonyConstants {

    /**
     * Copied from {@link android.telephony.TelephonyManager}
     */
    public static class TelephonyManagerConstants {

        // Network modes are in turn copied from RILConstants
        // with one difference: NETWORK_MODE_CDMA is named NETWORK_MODE_CDMA_EVDO

        /** GSM, WCDMA (WCDMA preferred) */
        public static final int NETWORK_MODE_WCDMA_PREF = 0;

        /** GSM only */
        public static final int NETWORK_MODE_GSM_ONLY = 1;

        /** WCDMA only */
        public static final int NETWORK_MODE_WCDMA_ONLY = 2;

        /** GSM, WCDMA (auto mode, according to PRL) */
        public static final int NETWORK_MODE_GSM_UMTS = 3;

        /** CDMA and EvDo (auto mode, according to PRL)
         * this is NETWORK_MODE_CDMA in RILConstants.java
         */
        public static final int NETWORK_MODE_CDMA_EVDO = 4;

        /** CDMA only */
        public static final int NETWORK_MODE_CDMA_NO_EVDO = 5;

        /** EvDo only */
        public static final int NETWORK_MODE_EVDO_NO_CDMA = 6;

        /** GSM, WCDMA, CDMA, and EvDo (auto mode, according to PRL) */
        public static final int NETWORK_MODE_GLOBAL = 7;

        /** LTE, CDMA and EvDo */
        public static final int NETWORK_MODE_LTE_CDMA_EVDO = 8;

        /** LTE, GSM and WCDMA */
        public static final int NETWORK_MODE_LTE_GSM_WCDMA = 9;

        /** LTE, CDMA, EvDo, GSM, and WCDMA */
        public static final int NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA = 10;

        /** LTE only mode. */
        public static final int NETWORK_MODE_LTE_ONLY = 11;

        /** LTE and WCDMA */
        public static final int NETWORK_MODE_LTE_WCDMA = 12;

        /** TD-SCDMA only */
        public static final int NETWORK_MODE_TDSCDMA_ONLY = 13;

        /** TD-SCDMA and WCDMA */
        public static final int NETWORK_MODE_TDSCDMA_WCDMA = 14;

        /** LTE and TD-SCDMA*/
        public static final int NETWORK_MODE_LTE_TDSCDMA = 15;

        /** TD-SCDMA and GSM */
        public static final int NETWORK_MODE_TDSCDMA_GSM = 16;

        /** TD-SCDMA, GSM and LTE */
        public static final int NETWORK_MODE_LTE_TDSCDMA_GSM = 17;

        /** TD-SCDMA, GSM and WCDMA */
        public static final int NETWORK_MODE_TDSCDMA_GSM_WCDMA = 18;

        /** LTE, TD-SCDMA and WCDMA */
        public static final int NETWORK_MODE_LTE_TDSCDMA_WCDMA = 19;

        /** LTE, TD-SCDMA, GSM, and WCDMA */
        public static final int NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA = 20;

        /** TD-SCDMA, CDMA, EVDO, GSM and WCDMA */
        public static final int NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA = 21;

        /** LTE, TDCSDMA, CDMA, EVDO, GSM and WCDMA */
        public static final int NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA = 22;

        /** NR 5G only mode */
        public static final int NETWORK_MODE_NR_ONLY = 23;

        /** NR 5G, LTE */
        public static final int NETWORK_MODE_NR_LTE = 24;

        /** NR 5G, LTE, CDMA and EvDo */
        public static final int NETWORK_MODE_NR_LTE_CDMA_EVDO = 25;

        /** NR 5G, LTE, GSM and WCDMA */
        public static final int NETWORK_MODE_NR_LTE_GSM_WCDMA = 26;

        /** NR 5G, LTE, CDMA, EvDo, GSM and WCDMA */
        public static final int NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA = 27;

        /** NR 5G, LTE and WCDMA */
        public static final int NETWORK_MODE_NR_LTE_WCDMA = 28;

        /** NR 5G, LTE and TDSCDMA */
        public static final int NETWORK_MODE_NR_LTE_TDSCDMA = 29;

        /** NR 5G, LTE, TD-SCDMA and GSM */
        public static final int NETWORK_MODE_NR_LTE_TDSCDMA_GSM = 30;

        /** NR 5G, LTE, TD-SCDMA, WCDMA */
        public static final int NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA = 31;

        /** NR 5G, LTE, TD-SCDMA, GSM and WCDMA */
        public static final int NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA = 32;

        /** NR 5G, LTE, TD-SCDMA, CDMA, EVDO, GSM and WCDMA */
        public static final int NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA = 33;


        // Grouping of RAFs
        // imported from {@link android.telephony.RadioAccessFamily}
        // 2G
        public static final long GSM = TelephonyManager.NETWORK_TYPE_BITMASK_GSM
                | TelephonyManager.NETWORK_TYPE_BITMASK_GPRS
                | TelephonyManager.NETWORK_TYPE_BITMASK_EDGE;
        public static final long CDMA = TelephonyManager.NETWORK_TYPE_BITMASK_CDMA
                | TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT;

        // 3G
        public static final long EVDO = TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_0
                | TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_A
                | TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_B
                | TelephonyManager.NETWORK_TYPE_BITMASK_EHRPD;
        public static final long HS = TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA
                | TelephonyManager.NETWORK_TYPE_BITMASK_HSDPA
                | TelephonyManager.NETWORK_TYPE_BITMASK_HSPA
                | TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP;
        public static final long WCDMA = HS | TelephonyManager.NETWORK_TYPE_BITMASK_UMTS;

        // 4G
        public static final long LTE = TelephonyManager.NETWORK_TYPE_BITMASK_LTE
                | TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA;

        // 5G
        public static final long NR = TelephonyManager.NETWORK_TYPE_BITMASK_NR;

    }


}
