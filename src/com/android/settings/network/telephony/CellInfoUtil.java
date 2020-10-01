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

package com.android.settings.network.telephony;

import android.telephony.CellIdentity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;

import com.android.internal.telephony.OperatorInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Add static Utility functions to get information from the CellInfo object.
 * TODO: Modify {@link CellInfo} for simplify those functions
 */
public final class CellInfoUtil {
    private static final String TAG = "NetworkSelectSetting";

    private CellInfoUtil() {
    }

    /**
     * Returns the title of the network obtained in the manual search.
     *
     * @param cellId contains the identity of the network.
     * @param networkMccMnc contains the MCCMNC string of the network
     * @return Long Name if not null/empty, otherwise Short Name if not null/empty,
     * else MCCMNC string.
     */
    public static String getNetworkTitle(CellIdentity cellId, String networkMccMnc) {
        if (cellId != null) {
            String title = Objects.toString(cellId.getOperatorAlphaLong(), "");
            if (TextUtils.isEmpty(title)) {
                title = Objects.toString(cellId.getOperatorAlphaShort(), "");
            }
            if (!TextUtils.isEmpty(title)) {
                return title;
            }
        }
        if (TextUtils.isEmpty(networkMccMnc)) {
            return "";
        }
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        return bidiFormatter.unicodeWrap(networkMccMnc, TextDirectionHeuristics.LTR);
    }

    /**
     * Returns the CellIdentity from CellInfo
     *
     * @param cellInfo contains the information of the network.
     * @return CellIdentity within CellInfo
     */
    public static CellIdentity getCellIdentity(CellInfo cellInfo) {
        if (cellInfo == null) {
            return null;
        }
        CellIdentity cellId = null;
        if (cellInfo instanceof CellInfoGsm) {
            cellId = ((CellInfoGsm) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoCdma) {
            cellId = ((CellInfoCdma) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoWcdma) {
            cellId = ((CellInfoWcdma) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoTdscdma) {
            cellId = ((CellInfoTdscdma) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoLte) {
            cellId = ((CellInfoLte) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoNr) {
            cellId = ((CellInfoNr) cellInfo).getCellIdentity();
        }
        return cellId;
    }

    /**
     * Creates a CellInfo object from OperatorInfo. GsmCellInfo is used here only because
     * operatorInfo does not contain technology type while CellInfo is an abstract object that
     * requires to specify technology type. It doesn't matter which CellInfo type to use here, since
     * we only want to wrap the operator info and PLMN to a CellInfo object.
     */
    public static CellInfo convertOperatorInfoToCellInfo(OperatorInfo operatorInfo) {
        final String operatorNumeric = operatorInfo.getOperatorNumeric();
        String mcc = null;
        String mnc = null;
        if (operatorNumeric != null && operatorNumeric.matches("^[0-9]{5,6}$")) {
            mcc = operatorNumeric.substring(0, 3);
            mnc = operatorNumeric.substring(3);
        }
        final CellIdentityGsm cig = new CellIdentityGsm(
                Integer.MAX_VALUE /* lac */,
                Integer.MAX_VALUE /* cid */,
                Integer.MAX_VALUE /* arfcn */,
                Integer.MAX_VALUE /* bsic */,
                mcc,
                mnc,
                operatorInfo.getOperatorAlphaLong(),
                operatorInfo.getOperatorAlphaShort(),
                Collections.emptyList());

        final CellInfoGsm ci = new CellInfoGsm();
        ci.setCellIdentity(cig);
        return ci;
    }

    /** Convert a list of cellInfos to readable string without sensitive info. */
    public static String cellInfoListToString(List<CellInfo> cellInfos) {
        return cellInfos.stream()
                .map(cellInfo -> cellInfoToString(cellInfo))
                .collect(Collectors.joining(", "));
    }

    /** Convert {@code cellInfo} to a readable string without sensitive info. */
    public static String cellInfoToString(CellInfo cellInfo) {
        final String cellType = cellInfo.getClass().getSimpleName();
        final CellIdentity cid = getCellIdentity(cellInfo);
        String mcc = null;
        String mnc = null;
        if (cid != null) {
            if (cid instanceof CellIdentityGsm) {
                mcc = ((CellIdentityGsm) cid).getMccString();
                mnc = ((CellIdentityGsm) cid).getMncString();
            } else if (cid instanceof CellIdentityWcdma) {
                mcc = ((CellIdentityWcdma) cid).getMccString();
                mnc = ((CellIdentityWcdma) cid).getMncString();
            } else if (cid instanceof CellIdentityTdscdma) {
                mcc = ((CellIdentityTdscdma) cid).getMccString();
                mnc = ((CellIdentityTdscdma) cid).getMncString();
            } else if (cid instanceof CellIdentityLte) {
                mcc = ((CellIdentityLte) cid).getMccString();
                mnc = ((CellIdentityLte) cid).getMncString();
            } else if (cid instanceof CellIdentityNr) {
                mcc = ((CellIdentityNr) cid).getMccString();
                mnc = ((CellIdentityNr) cid).getMncString();
            }
        }
        return String.format(
                "{CellType = %s, isRegistered = %b, mcc = %s, mnc = %s, alphaL = %s, alphaS = %s}",
                cellType, cellInfo.isRegistered(), mcc, mnc,
                cid.getOperatorAlphaLong(), cid.getOperatorAlphaShort());
    }
}
