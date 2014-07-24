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

import com.android.settings.R;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;

public class UserPreference extends Preference {

    public static final int USERID_UNKNOWN = -10;
    public static final int USERID_GUEST_DEFAULTS = -11;

    private OnClickListener mDeleteClickListener;
    private OnClickListener mSettingsClickListener;
    private int mSerialNumber = -1;
    private int mUserId = USERID_UNKNOWN;
    static final int SETTINGS_ID = R.id.manage_user;
    static final int DELETE_ID = R.id.trash_user;

    public UserPreference(Context context, AttributeSet attrs) {
        this(context, attrs, USERID_UNKNOWN, null, null);
    }

    UserPreference(Context context, AttributeSet attrs, int userId,
            OnClickListener settingsListener,
            OnClickListener deleteListener) {
        super(context, attrs);
        if (deleteListener != null || settingsListener != null) {
            setWidgetLayoutResource(R.layout.preference_user_delete_widget);
        }
        mDeleteClickListener = deleteListener;
        mSettingsClickListener = settingsListener;
        mUserId = userId;
    }

    @Override
    protected void onBindView(View view) {
        UserManager um = (UserManager) getContext().getSystemService(Context.USER_SERVICE);
        View deleteDividerView = view.findViewById(R.id.divider_delete);
        View manageDividerView = view.findViewById(R.id.divider_manage);
        View deleteView = view.findViewById(R.id.trash_user);
        if (deleteView != null) {
            if (mDeleteClickListener != null
                    && !um.hasUserRestriction(UserManager.DISALLOW_REMOVE_USER)) {
                deleteView.setOnClickListener(mDeleteClickListener);
                deleteView.setTag(this);
            } else {
                deleteView.setVisibility(View.GONE);
                deleteDividerView.setVisibility(View.GONE);
            }
        }
        View manageView = view.findViewById(R.id.manage_user);
        if (manageView != null) {
            if (mSettingsClickListener != null) {
                manageView.setOnClickListener(mSettingsClickListener);
                manageView.setTag(this);
                if (mDeleteClickListener != null) {
                    manageDividerView.setVisibility(View.GONE);
                }
            } else {
                manageView.setVisibility(View.GONE);
                manageDividerView.setVisibility(View.GONE);
            }
        }
        super.onBindView(view);
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

    public int compareTo(Preference another) {
        if (another instanceof UserPreference) {
            return getSerialNumber() > ((UserPreference) another).getSerialNumber() ? 1 : -1;
        } else {
            return 1;
        }
    }
}
