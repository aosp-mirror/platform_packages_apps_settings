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

package com.android.settings.widget;

import android.content.Context;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.widget.AppPreference;

/**
 * {@link AppPreference} that implements user restriction utilities using
 * {@link com.android.settingslib.RestrictedPreferenceHelper}.
 * Used to show policy transparency on {@link AppPreference}.
 */
public class RestrictedAppPreference extends AppPreference {
    private RestrictedPreferenceHelper mHelper;
    private String userRestriction;

    public RestrictedAppPreference(Context context) {
        super(context);
        initialize(null, null);
    }

    public RestrictedAppPreference(Context context, String userRestriction) {
        super(context);
        initialize(null, userRestriction);
    }

    public RestrictedAppPreference(Context context, AttributeSet attrs, String userRestriction) {
        super(context, attrs);
        initialize(attrs, userRestriction);
    }

    private void initialize(AttributeSet attrs, String userRestriction) {
        mHelper = new RestrictedPreferenceHelper(getContext(), this, attrs);
        this.userRestriction = userRestriction;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mHelper.onBindViewHolder(holder);
    }

    @Override
    public void performClick() {
        if (!mHelper.performClick()) {
            super.performClick();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        boolean changed = false;
        if (enabled && isDisabledByAdmin()) {
            mHelper.setDisabledByAdmin(null);
            changed = true;
        }
        if (enabled && isDisabledByEcm()) {
            mHelper.setDisabledByEcm(null);
            changed = true;
        }
        if (!changed) {
            super.setEnabled(enabled);
        }
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        if (mHelper.setDisabledByAdmin(admin)) {
            notifyChanged();
        }
    }

    public boolean isDisabledByAdmin() {
        return mHelper.isDisabledByAdmin();
    }

    public boolean isDisabledByEcm() {
        return mHelper.isDisabledByEcm();
    }

    public void useAdminDisabledSummary(boolean useSummary) {
        mHelper.useAdminDisabledSummary(useSummary);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        mHelper.onAttachedToHierarchy();
        super.onAttachedToHierarchy(preferenceManager);
    }

    public void checkRestrictionAndSetDisabled() {
        if (TextUtils.isEmpty(userRestriction)) {
            return;
        }
        mHelper.checkRestrictionAndSetDisabled(userRestriction, UserHandle.myUserId());
    }

    public void checkRestrictionAndSetDisabled(String userRestriction) {
        mHelper.checkRestrictionAndSetDisabled(userRestriction, UserHandle.myUserId());
    }

    public void checkRestrictionAndSetDisabled(String userRestriction, int userId) {
        mHelper.checkRestrictionAndSetDisabled(userRestriction, userId);
    }

    /**
     * Checks if the given setting is subject to Enhanced Confirmation Mode restrictions for this
     * package. Marks the preference as disabled if so.
     * @param settingIdentifier The key identifying the setting
     * @param packageName the package to check the settingIdentifier for
     */
    public void checkEcmRestrictionAndSetDisabled(@NonNull String settingIdentifier,
                                                  @NonNull String packageName) {
        mHelper.checkEcmRestrictionAndSetDisabled(settingIdentifier, packageName);
    }
}
