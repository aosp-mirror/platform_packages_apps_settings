/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.users;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.AttributeSet;

import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.RestrictedPreference;

import java.util.Comparator;

/**
 * Preference for a user that appear on {@link UserSettings} screen.
 */
public class UserPreference extends RestrictedPreference {
    private static final int ALPHA_ENABLED = 255;
    private static final int ALPHA_DISABLED = 102;

    public static final int USERID_UNKNOWN = -10;
    public static final int USERID_GUEST_DEFAULTS = -11;
    public static final Comparator<UserPreference> SERIAL_NUMBER_COMPARATOR =
            (p1, p2) -> {

                if (p1 == null) {
                    return -1;
                } else if (p2 == null) {
                    return 1;
                }
                int sn1 = p1.getSerialNumber();
                int sn2 = p2.getSerialNumber();
                if (sn1 < sn2) {
                    return -1;
                } else if (sn1 > sn2) {
                    return 1;
                }
                return 0;
            };

    private int mSerialNumber = -1;
    private int mUserId = USERID_UNKNOWN;

    public UserPreference(Context context, AttributeSet attrs) {
        this(context, attrs, USERID_UNKNOWN);
    }

    UserPreference(Context context, AttributeSet attrs, int userId) {
        super(context, attrs);
        mUserId = userId;
        useAdminDisabledSummary(true);
    }

    private void dimIcon(boolean dimmed) {
        Drawable icon = getIcon();
        if (icon != null) {
            icon.mutate().setAlpha(dimmed ? ALPHA_DISABLED : ALPHA_ENABLED);
            setIcon(icon);
        }
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return true;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        dimIcon(isDisabledByAdmin());
    }

    private int getSerialNumber() {
        if (mUserId == UserHandle.myUserId()) return Integer.MIN_VALUE;
        if (mSerialNumber < 0) {
            // If the userId is unknown
            if (mUserId == USERID_UNKNOWN) {
                return Integer.MAX_VALUE;
            } else if (mUserId == USERID_GUEST_DEFAULTS) {
                return Integer.MAX_VALUE - 1;
            }
            mSerialNumber = ((UserManager) getContext().getSystemService(Context.USER_SERVICE))
                    .getUserSerialNumber(mUserId);
            if (mSerialNumber < 0) return mUserId;
        }
        return mSerialNumber;
    }

    public int getUserId() {
        return mUserId;
    }
}
