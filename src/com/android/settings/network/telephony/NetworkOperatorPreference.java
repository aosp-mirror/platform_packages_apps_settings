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

import static android.telephony.SignalStrength.NUM_SIGNAL_STRENGTH_BINS;

import android.content.Context;
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
import android.telephony.CellSignalStrength;
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.telephony.OperatorInfo;
import com.android.settings.R;

import java.util.List;
import java.util.Objects;

/**
 * A Preference represents a network operator in the NetworkSelectSetting fragment.
 */
public class NetworkOperatorPreference extends Preference {

    private static final String TAG = "NetworkOperatorPref";
    private static final boolean DBG = false;

    private static final int LEVEL_NONE = -1;

    private CellInfo mCellInfo;
    private CellIdentity mCellId;
    private List<String> mForbiddenPlmns;
    private int mLevel = LEVEL_NONE;
    private boolean mShow4GForLTE;
    private boolean mUseNewApi;

    public NetworkOperatorPreference(Context context, CellInfo cellinfo,
            List<String> forbiddenPlmns, boolean show4GForLTE) {
        this(context, forbiddenPlmns, show4GForLTE);
        updateCell(cellinfo);
    }

    public NetworkOperatorPreference(Context context, CellIdentity connectedCellId,
            List<String> forbiddenPlmns, boolean show4GForLTE) {
        this(context, forbiddenPlmns, show4GForLTE);
        updateCell(null, connectedCellId);
    }

    private NetworkOperatorPreference(
            Context context, List<String> forbiddenPlmns, boolean show4GForLTE) {
        super(context);
        mForbiddenPlmns = forbiddenPlmns;
        mShow4GForLTE = show4GForLTE;
        mUseNewApi = context.getResources().getBoolean(
                com.android.internal.R.bool.config_enableNewAutoSelectNetworkUI);
    }

    /**
     * Change cell information
     */
    public void updateCell(CellInfo cellinfo) {
        updateCell(cellinfo, CellInfoUtil.getCellIdentity(cellinfo));
    }

    private void updateCell(CellInfo cellinfo, CellIdentity cellId) {
        mCellInfo = cellinfo;
        mCellId = cellId;
        refresh();
    }

    /**
     * Compare cell within preference
     */
    public boolean isSameCell(CellInfo cellinfo) {
        if (cellinfo == null) {
            return false;
        }
        return mCellId.equals(CellInfoUtil.getCellIdentity(cellinfo));
    }

    /**
     * Refresh the NetworkOperatorPreference by updating the title and the icon.
     */
    public void refresh() {
        String networkTitle = getOperatorName();

        if ((mForbiddenPlmns != null) && mForbiddenPlmns.contains(getOperatorNumeric())) {
            if (DBG) Log.d(TAG, "refresh forbidden network: " + networkTitle);
            networkTitle += " "
                    + getContext().getResources().getString(R.string.forbidden_network);
        } else {
            if (DBG) Log.d(TAG, "refresh the network: " + networkTitle);
        }
        setTitle(Objects.toString(networkTitle, ""));

        if (mCellInfo == null) {
            return;
        }

        final CellSignalStrength signalStrength = getCellSignalStrength(mCellInfo);
        final int level = signalStrength != null ? signalStrength.getLevel() : LEVEL_NONE;
        if (DBG) Log.d(TAG, "refresh level: " + String.valueOf(level));
        mLevel = level;
        updateIcon(mLevel);
    }

    /**
     * Update the icon according to the input signal strength level.
     */
    public void setIcon(int level) {
        updateIcon(level);
    }

    /**
     * Operator numeric of this cell
     */
    public String getOperatorNumeric() {
        final CellIdentity cellId = mCellId;
        if (cellId == null) {
            return null;
        }
        if (cellId instanceof CellIdentityGsm) {
            return ((CellIdentityGsm) cellId).getMobileNetworkOperator();
        }
        if (cellId instanceof CellIdentityWcdma) {
            return ((CellIdentityWcdma) cellId).getMobileNetworkOperator();
        }
        if (cellId instanceof CellIdentityTdscdma) {
            return ((CellIdentityTdscdma) cellId).getMobileNetworkOperator();
        }
        if (cellId instanceof CellIdentityLte) {
            return ((CellIdentityLte) cellId).getMobileNetworkOperator();
        }
        if (cellId instanceof CellIdentityNr) {
            final String mcc = ((CellIdentityNr) cellId).getMccString();
            if (mcc == null) {
                return null;
            }
            return mcc.concat(((CellIdentityNr) cellId).getMncString());
        }
        return null;
    }

    /**
     * Operator name of this cell
     */
    public String getOperatorName() {
        return CellInfoUtil.getNetworkTitle(mCellId, getOperatorNumeric());
    }

    /**
     * Operator info of this cell
     */
    public OperatorInfo getOperatorInfo() {
        return new OperatorInfo(Objects.toString(mCellId.getOperatorAlphaLong(), ""),
                Objects.toString(mCellId.getOperatorAlphaShort(), ""),
                getOperatorNumeric());
    }

    private int getIconIdForCell(CellInfo ci) {
        if (ci instanceof CellInfoGsm) {
            return R.drawable.signal_strength_g;
        }
        if (ci instanceof CellInfoCdma) {
            return R.drawable.signal_strength_1x;
        }
        if ((ci instanceof CellInfoWcdma) || (ci instanceof CellInfoTdscdma)) {
            return R.drawable.signal_strength_3g;
        }
        if (ci instanceof CellInfoLte) {
            return mShow4GForLTE
                    ? R.drawable.ic_signal_strength_4g : R.drawable.signal_strength_lte;
        }
        if (ci instanceof CellInfoNr) {
            return R.drawable.signal_strength_5g;
        }
        return MobileNetworkUtils.NO_CELL_DATA_TYPE_ICON;
    }

    private CellSignalStrength getCellSignalStrength(CellInfo ci) {
        if (ci instanceof CellInfoGsm) {
            return ((CellInfoGsm) ci).getCellSignalStrength();
        }
        if (ci instanceof CellInfoCdma) {
            return ((CellInfoCdma) ci).getCellSignalStrength();
        }
        if (ci instanceof CellInfoWcdma) {
            return ((CellInfoWcdma) ci).getCellSignalStrength();
        }
        if (ci instanceof CellInfoTdscdma) {
            return ((CellInfoTdscdma) ci).getCellSignalStrength();
        }
        if (ci instanceof CellInfoLte) {
            return ((CellInfoLte) ci).getCellSignalStrength();
        }
        if (ci instanceof CellInfoNr) {
            return ((CellInfoNr) ci).getCellSignalStrength();
        }
        return null;
    }

    private void updateIcon(int level) {
        if (!mUseNewApi || level < 0 || level >= NUM_SIGNAL_STRENGTH_BINS) {
            return;
        }
        final Context context = getContext();
        setIcon(MobileNetworkUtils.getSignalStrengthIcon(context, level, NUM_SIGNAL_STRENGTH_BINS,
                getIconIdForCell(mCellInfo), false));
    }
}
