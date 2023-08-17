/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.widget.Button;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

/**
 * A preference with a plus button on the side representing an "add" action. The plus button will
 * only be visible when a non-null click listener is registered.
 */
public class RestrictedButton extends Button {

    private UserHandle mUserHandle;
    private String mUserRestriction;

    public RestrictedButton(Context context) {
        super(context);
    }

    public RestrictedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RestrictedButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RestrictedButton(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean performClick() {
        EnforcedAdmin admin = getEnforcedAdmin();
        if (admin != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext, admin);
            return false;
        }
        return super.performClick();
    }

    /** Initialize the button with {@link UserHandle} and a restriction */
    public void init(UserHandle userHandle, String restriction) {
        setAllowClickWhenDisabled(true);
        mUserHandle = userHandle;
        mUserRestriction = restriction;
    }

    /** Update the restriction state */
    public void updateState() {
        setEnabled(getEnforcedAdmin() == null);
    }

    private EnforcedAdmin getEnforcedAdmin() {
        if (mUserHandle != null) {
            EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                    mContext, mUserRestriction, mUserHandle.getIdentifier());
            if (admin != null) {
                return admin;
            }
        }
        return null;
    }
}
