/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.network;

import android.app.Activity;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.enterprise.ActionDisabledByAdminDialogHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * A builder for creating restriction View when constructing UI for resetting network
 * configuration.
 */
public class ResetNetworkRestrictionViewBuilder extends NetworkResetRestrictionChecker {

    @VisibleForTesting
    static final String mRestriction = UserManager.DISALLOW_NETWORK_RESET;

    protected Activity mActivity;
    protected LayoutInflater mInflater;

    /**
     * Constructor of builder.
     *
     * @param activity Activity to present restriction View.
     */
    public ResetNetworkRestrictionViewBuilder(Activity activity) {
        super(activity);
        mActivity = activity;
    }

    /**
     * Option for configuring LayoutInflater.
     *
     * @param inflater LayoutInflater
     * @return this builder
     */
    public ResetNetworkRestrictionViewBuilder setLayoutInflater(LayoutInflater inflater) {
        mInflater = inflater;
        return this;
    }

    /**
     * Try to provide a View if access to reset network is not allowed.
     * @return a View which presenting information of restrictions.
     *         {@code null} when no restriction on accessing.
     */
    public View build() {
        if (hasUserRestriction()) {
            return operationNotAllow();
        }

        // Not allow when this option is restricted.
        EnforcedAdmin admin = getEnforceAdminByRestriction();
        if (admin == null) {
            return null;
        }

        createRestrictDialogBuilder(admin)
                .setOnDismissListener(dialogInterface -> mActivity.finish())
                .show();
        return createEmptyView();
    }

    @VisibleForTesting
    protected LayoutInflater getLayoutInflater() {
        if (mInflater != null) {
            return mInflater;
        }
        return mActivity.getLayoutInflater();
    }

    @VisibleForTesting
    protected View operationNotAllow() {
        return getLayoutInflater().inflate(R.layout.network_reset_disallowed_screen, null);
    }

    @VisibleForTesting
    protected EnforcedAdmin getEnforceAdminByRestriction() {
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mActivity, mRestriction, UserHandle.myUserId());
    }

    @VisibleForTesting
    protected AlertDialog.Builder createRestrictDialogBuilder(EnforcedAdmin admin) {
        return (new ActionDisabledByAdminDialogHelper(mActivity))
                .prepareDialogBuilder(mRestriction, admin);
    }

    @VisibleForTesting
    protected View createEmptyView() {
        return new ViewStub(mActivity);
    }
}
