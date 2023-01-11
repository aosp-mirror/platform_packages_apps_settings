/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.vpn2;

import static android.text.Spanned.SPAN_EXCLUSIVE_INCLUSIVE;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import com.android.settings.R;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.Utils;

/**
 * This class sets appropriate enabled state and user admin message when userId is set
 */
public abstract class ManageablePreference extends GearPreference {

    public static int STATE_NONE = -1;

    boolean mIsAlwaysOn = false;
    boolean mIsInsecureVpn = false;
    int mState = STATE_NONE;
    int mUserId;

    public ManageablePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public ManageablePreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setPersistent(false);
        setOrder(0);
        setUserId(UserHandle.myUserId());
    }

    public int getUserId() {
        return mUserId;
    }

    public void setUserId(int userId) {
        mUserId = userId;
        checkRestrictionAndSetDisabled(UserManager.DISALLOW_CONFIG_VPN, userId);
    }

    public boolean isAlwaysOn() {
        return mIsAlwaysOn;
    }

    public boolean isInsecureVpn() {
        return mIsInsecureVpn;
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        if (mState != state) {
            mState = state;
            updateSummary();
            notifyHierarchyChanged();
        }
    }

    public void setAlwaysOn(boolean isEnabled) {
        if (mIsAlwaysOn != isEnabled) {
            mIsAlwaysOn = isEnabled;
            updateSummary();
        }
    }

    /**
     * Set whether the VPN associated with this preference has an insecure type.
     * By default the value will be False.
     */
    public void setInsecureVpn(boolean isInsecureVpn) {
        if (mIsInsecureVpn != isInsecureVpn) {
            mIsInsecureVpn = isInsecureVpn;
            updateSummary();
        }
    }

    /**
     * Update the preference summary string (see {@see Preference#setSummary}) with a string
     * reflecting connection status, always-on setting and whether the vpn is insecure.
     *
     * State is not shown for {@code STATE_NONE}.
     */
    protected void updateSummary() {
        final Resources res = getContext().getResources();
        final String[] states = res.getStringArray(R.array.vpn_states);
        String summary = (mState == STATE_NONE ? "" : states[mState]);
        if (mIsInsecureVpn) {
            final String insecureString = res.getString(R.string.vpn_insecure_summary);
            summary = TextUtils.isEmpty(summary) ? insecureString : summary + " / "
                    + insecureString;

            SpannableString summarySpan = new SpannableString(summary);
            final int colorError = Utils.getColorErrorDefaultColor(getContext());
            summarySpan.setSpan(new ForegroundColorSpan(colorError), 0, summary.length(),
                    SPAN_EXCLUSIVE_INCLUSIVE);
            setSummary(summarySpan);
        } else if (mIsAlwaysOn) {
            final String alwaysOnString = res.getString(R.string.vpn_always_on_summary_active);
            summary = TextUtils.isEmpty(summary) ? alwaysOnString : summary + " / "
                    + alwaysOnString;
            setSummary(summary);
        } else {
            setSummary(summary);
        }
    }
}
