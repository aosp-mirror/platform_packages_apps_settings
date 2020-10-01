/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.utils.ColorUtil;

public class RestrictedRadioButton extends RadioButton {
    private Context mContext;
    private boolean mDisabledByAdmin;
    private EnforcedAdmin mEnforcedAdmin;

    public RestrictedRadioButton(Context context) {
        this(context, null);
    }

    public RestrictedRadioButton(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.radioButtonStyle);
    }

    public RestrictedRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RestrictedRadioButton(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }

    @Override
    public boolean performClick() {
        if (mDisabledByAdmin) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext, mEnforcedAdmin);
            return true;
        }
        return super.performClick();
    }

    public void setDisabledByAdmin(EnforcedAdmin admin) {
        final boolean disabled = (admin != null);
        mEnforcedAdmin = admin;
        if (mDisabledByAdmin != disabled) {
            mDisabledByAdmin = disabled;
            RestrictedLockUtilsInternal.setTextViewAsDisabledByAdmin(mContext,
                    (TextView) this, mDisabledByAdmin);
            if (mDisabledByAdmin) {
                getButtonDrawable().setAlpha(
                        (int) (255 * ColorUtil.getDisabledAlpha(mContext)));
            } else {
                getButtonDrawable().setAlpha(0);
            }
        }
    }

    public boolean isDisabledByAdmin() {
        return mDisabledByAdmin;
    }
}