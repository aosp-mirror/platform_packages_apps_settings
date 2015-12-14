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
 * limitations under the License
 */

package com.android.settings;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

/**
 * Helper class for managing settings preferences that can be disabled
 * by device admins via user restrictions.
 *
 **/
public class RestrictedPreferenceHelper {
    private final Context mContext;
    private final Preference mPreference;
    private final Drawable mRestrictedPadlock;
    private final int mRestrictedPadlockPadding;
    private final DevicePolicyManager mDevicePolicyManager;

    private boolean mDisabledByAdmin;
    private ComponentName mEnforcedAdmin;
    private int mUserId = UserHandle.USER_NULL;
    private String mAttrUserRestriction = null;

    RestrictedPreferenceHelper(Context context, Preference preference,
            AttributeSet attrs) {
        mContext = context;
        mPreference = preference;

        mRestrictedPadlock = mContext.getDrawable(R.drawable.ic_settings_lock_outline);
        final int iconSize = mContext.getResources().getDimensionPixelSize(
                R.dimen.restricted_lock_icon_size);
        mRestrictedPadlock.setBounds(0, 0, iconSize, iconSize);
        mRestrictedPadlockPadding = mContext.getResources().getDimensionPixelSize(
                R.dimen.restricted_lock_icon_padding);

        mDevicePolicyManager = (DevicePolicyManager) mContext.getSystemService(
                Context.DEVICE_POLICY_SERVICE);

        mAttrUserRestriction = attrs.getAttributeValue(
                R.styleable.RestrictedPreference_userRestriction);
        final TypedArray attributes = context.obtainStyledAttributes(attrs,
                R.styleable.RestrictedPreference);
        final TypedValue userRestriction =
                attributes.peekValue(R.styleable.RestrictedPreference_userRestriction);
        CharSequence data = null;
        if (userRestriction != null && userRestriction.type == TypedValue.TYPE_STRING) {
            if (userRestriction.resourceId != 0) {
                data = context.getText(userRestriction.resourceId);
            } else {
                data = userRestriction.string;
            }
        }
        mAttrUserRestriction = data == null ? null : data.toString();
    }

    /**
     * Modify PreferenceViewHolder to add padlock if restriction is disabled.
     */
    public void onBindViewHolder(PreferenceViewHolder holder) {
        final TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        if (titleView != null) {
            if (mDisabledByAdmin) {
                titleView.setCompoundDrawablesRelative(null, null, mRestrictedPadlock, null);
                titleView.setCompoundDrawablePadding(mRestrictedPadlockPadding);
                holder.itemView.setEnabled(true);
            } else {
                titleView.setCompoundDrawablesRelative(null, null, null, null);
            }
        }
    }

    /**
     * Check if the preference is disabled if so handle the click by informing the user.
     *
     * @return true if the method handled the click.
     */
    public boolean performClick() {
        if (mDisabledByAdmin) {
            Intent intent = new Intent(mContext, ShowAdminSupportDetailsDialog.class);
            if (mEnforcedAdmin != null) {
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mEnforcedAdmin);
            }
            if (mUserId != UserHandle.USER_NULL) {
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            }
            mContext.startActivity(intent);
            return true;
        }
        return false;
    }

    /**
     * Disable / enable if we have been passed the restriction in the xml.
     */
    protected void onAttachedToHierarchy() {
        if (mAttrUserRestriction != null) {
            checkRestrictionAndSetDisabled(mAttrUserRestriction, UserHandle.myUserId());
        }
    }

    /**
     * Set the user restriction that is used to disable this preference.
     *
     * @param userRestriction constant from {@link android.os.UserManager}
     * @param userId user to check the restriction for.
     */
    public void checkRestrictionAndSetDisabled(String userRestriction, int userId) {
        ComponentName deviceOwner = mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser();
        int deviceOwnerUserId = mDevicePolicyManager.getDeviceOwnerUserId();
        boolean enforcedByDeviceOwner = false;
        if (deviceOwner != null && deviceOwnerUserId != UserHandle.USER_NULL) {
            enforcedByDeviceOwner = isEnforcedByAdmin(
                    deviceOwner, userRestriction, deviceOwnerUserId);
        }

        ComponentName profileOwner = mDevicePolicyManager.getProfileOwnerAsUser(userId);
        boolean enforcedByProfileOwner = false;
        if (profileOwner != null && userId != UserHandle.USER_NULL) {
            enforcedByProfileOwner = isEnforcedByAdmin(
                    profileOwner, userRestriction, userId);
        }

        if (!enforcedByDeviceOwner && !enforcedByProfileOwner) {
            setDisabledByAdmin(false, null, UserHandle.USER_NULL);
            return;
        }

        if (enforcedByDeviceOwner && enforcedByProfileOwner) {
            setDisabledByAdmin(true, null, UserHandle.USER_NULL);
        } else if (enforcedByDeviceOwner) {
            setDisabledByAdmin(true, deviceOwner, deviceOwnerUserId);
        } else {
            setDisabledByAdmin(true, profileOwner, userId);
        }
    }

    private boolean isEnforcedByAdmin(ComponentName admin, String userRestriction, int userId) {
        Bundle enforcedRestrictions = mDevicePolicyManager.getUserRestrictions(admin, userId);
        if (enforcedRestrictions != null
                && enforcedRestrictions.getBoolean(userRestriction, false)) {
            return true;
        }
        return false;
    }

    /**
     * Disable this preference.
     *
     * @param disabled true if preference should be disabled.
     * @param admin Device admin that disabled the preference.
     * @param userId userId the device admin is installed for.
     * @return true if the disabled state was changed.
     */
    public boolean setDisabledByAdmin(boolean disabled, ComponentName admin, int userId) {
        if (mDisabledByAdmin != disabled) {
            mDisabledByAdmin = disabled;
            mEnforcedAdmin = admin;
            mUserId = userId;
            mPreference.setEnabled(!disabled);
            return true;
        }
        return false;
    }

    public boolean isDisabledByAdmin() {
        return mDisabledByAdmin;
    }
}
