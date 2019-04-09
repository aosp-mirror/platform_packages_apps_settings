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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

import java.util.Comparator;

public class UserPreference extends RestrictedPreference {
    private static final int ALPHA_ENABLED = 255;
    private static final int ALPHA_DISABLED = 102;

    public static final int USERID_UNKNOWN = -10;
    public static final int USERID_GUEST_DEFAULTS = -11;
    public static final Comparator<UserPreference> SERIAL_NUMBER_COMPARATOR =
            (p1, p2) -> {

                if (p1 == null) {
                    return -1;
                }
                else if (p2 == null) {
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
            setWidgetLayoutResource(R.layout.restricted_preference_user_delete_widget);
        }
        mDeleteClickListener = deleteListener;
        mSettingsClickListener = settingsListener;
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
        if (isDisabledByAdmin()) {
            // Disabled by admin, show no secondary target.
            return true;
        }
        if (canDeleteUser()) {
            // Need to show delete user target so don't hide.
            return false;
        }
        // Hide if don't have advanced setting listener.
        return mSettingsClickListener == null;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        final boolean disabledByAdmin = isDisabledByAdmin();
        dimIcon(disabledByAdmin);
        View userDeleteWidget = view.findViewById(R.id.user_delete_widget);
        if (userDeleteWidget != null) {
            userDeleteWidget.setVisibility(disabledByAdmin ? View.GONE : View.VISIBLE);
        }
        if (!disabledByAdmin) {
            View deleteDividerView = view.findViewById(R.id.divider_delete);
            View manageDividerView = view.findViewById(R.id.divider_manage);
            View deleteView = view.findViewById(R.id.trash_user);
            if (deleteView != null) {
                if (canDeleteUser()) {
                    deleteView.setVisibility(View.VISIBLE);
                    deleteDividerView.setVisibility(View.VISIBLE);
                    deleteView.setOnClickListener(mDeleteClickListener);
                    deleteView.setTag(this);
                } else {
                    deleteView.setVisibility(View.GONE);
                    deleteDividerView.setVisibility(View.GONE);
                }
            }
            ImageView manageView = (ImageView) view.findViewById(R.id.manage_user);
            if (manageView != null) {
                if (mSettingsClickListener != null) {
                    manageView.setVisibility(View.VISIBLE);
                    manageDividerView.setVisibility(mDeleteClickListener == null
                            ? View.VISIBLE : View.GONE);
                    manageView.setOnClickListener(mSettingsClickListener);
                    manageView.setTag(this);
                } else {
                    manageView.setVisibility(View.GONE);
                    manageDividerView.setVisibility(View.GONE);
                }
            }
        }
    }

    private boolean canDeleteUser() {
        return mDeleteClickListener != null
                && !RestrictedLockUtilsInternal.hasBaseUserRestriction(getContext(),
                UserManager.DISALLOW_REMOVE_USER, UserHandle.myUserId());
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
