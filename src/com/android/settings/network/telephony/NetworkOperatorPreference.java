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
import android.telephony.CellInfo;
import android.telephony.CellSignalStrength;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;

/**
 * A Preference represents a network operator in the NetworkSelectSetting fragment.
 */
public class NetworkOperatorPreference extends Preference {

    private static final String TAG = "NetworkOperatorPref";
    private static final boolean DBG = false;

    private static final int LEVEL_NONE = -1;

    private CellInfo mCellInfo;
    private List<String> mForbiddenPlmns;
    private int mLevel = LEVEL_NONE;
    private boolean mShow4GForLTE;
    private boolean mUseNewApi;

    public NetworkOperatorPreference(
            CellInfo cellinfo, Context context, List<String> forbiddenPlmns, boolean show4GForLTE) {
        super(context);
        mCellInfo = cellinfo;
        mForbiddenPlmns = forbiddenPlmns;
        mShow4GForLTE = show4GForLTE;
        mUseNewApi = context.getResources().getBoolean(
                com.android.internal.R.bool.config_enableNewAutoSelectNetworkUI);
        refresh();
    }

    public CellInfo getCellInfo() {
        return mCellInfo;
    }

    /**
     * Refresh the NetworkOperatorPreference by updating the title and the icon.
     */
    public void refresh() {
        if (DBG) Log.d(TAG, "refresh the network: " + CellInfoUtil.getNetworkTitle(mCellInfo));
        String networkTitle = CellInfoUtil.getNetworkTitle(mCellInfo);
        if (CellInfoUtil.isForbidden(mCellInfo, mForbiddenPlmns)) {
            networkTitle += " " + getContext().getResources().getString(R.string.forbidden_network);
        }
        setTitle(networkTitle);

        final CellSignalStrength signalStrength = mCellInfo.getCellSignalStrength();
        final int level = signalStrength != null ? signalStrength.getLevel() : LEVEL_NONE;
        if (DBG) Log.d(TAG, "refresh level: " + String.valueOf(level));
        if (mLevel != level) {
            mLevel = level;
            updateIcon(mLevel);
        }
    }

    /**
     * Update the icon according to the input signal strength level.
     */
    public void setIcon(int level) {
        updateIcon(level);
    }

    private int getIconIdForCell(CellInfo ci) {
        final int type = ci.getCellIdentity().getType();
        switch (type) {
            case CellInfo.TYPE_GSM:
                return R.drawable.signal_strength_g;
            case CellInfo.TYPE_WCDMA: // fall through
            case CellInfo.TYPE_TDSCDMA:
                return R.drawable.signal_strength_3g;
            case CellInfo.TYPE_LTE:
                return mShow4GForLTE
                        ? R.drawable.ic_signal_strength_4g : R.drawable.signal_strength_lte;
            case CellInfo.TYPE_CDMA:
                return R.drawable.signal_strength_1x;
            default:
                return MobileNetworkUtils.NO_CELL_DATA_TYPE_ICON;
        }
    }

    private void updateIcon(int level) {
        if (!mUseNewApi || level < 0 || level >= NUM_SIGNAL_STRENGTH_BINS) {
            return;
        }
        Context context = getContext();
        setIcon(MobileNetworkUtils.getSignalStrengthIcon(context, level, NUM_SIGNAL_STRENGTH_BINS,
                getIconIdForCell(mCellInfo), false));
    }
}
