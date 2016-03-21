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

import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.settings.GearPreference;
import com.android.settings.R;

/**
 * This class sets appropriate enabled state and user admin message when userId is set
 */
public abstract class ManageablePreference extends GearPreference {

    public static int STATE_NONE = -1;

    boolean mIsAlwaysOn = false;
    int mUserId;

    public ManageablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
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

    public void setAlwaysOn(boolean isEnabled) {
        mIsAlwaysOn = isEnabled;
    }

    /**
     * State is not shown for {@code STATE_NONE}
     *
     * @return summary string showing current connection state and always-on-vpn state
     */
    protected String getSummaryString(int state) {
        final Resources res = getContext().getResources();
        final String[] states = res.getStringArray(R.array.vpn_states);
        String summary = state == STATE_NONE ? "" : states[state];
        if (mIsAlwaysOn) {
            final String alwaysOnString = res.getString(R.string.vpn_always_on_active);
            summary = TextUtils.isEmpty(summary) ? alwaysOnString : res.getString(
                    R.string.join_two_unrelated_items, summary, alwaysOnString);
        }
        return summary;
    }
}
