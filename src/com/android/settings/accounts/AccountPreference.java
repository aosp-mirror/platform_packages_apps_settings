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
 * limitations under the License.
 */

package com.android.settings.accounts;

import android.accounts.Account;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import java.util.ArrayList;

/**
 * AccountPreference is used to display a username, status and provider icon for an account on
 * the device.
 */
public class AccountPreference extends Preference {
    private static final String TAG = "AccountPreference";
    public static final int SYNC_ENABLED = 0; // all know sync adapters are enabled and OK
    public static final int SYNC_DISABLED = 1; // no sync adapters are enabled
    public static final int SYNC_ERROR = 2; // one or more sync adapters have a problem
    public static final int SYNC_IN_PROGRESS = 3; // currently syncing
    private int mStatus;
    private Account mAccount;
    private ArrayList<String> mAuthorities;
    private ImageView mSyncStatusIcon;
    private boolean mShowTypeIcon;

    public AccountPreference(Context context, Account account, Drawable icon,
            ArrayList<String> authorities, boolean showTypeIcon) {
        super(context);
        mAccount = account;
        mAuthorities = authorities;
        mShowTypeIcon = showTypeIcon;
        if (showTypeIcon) {
            setIcon(icon);
        } else {
            setIcon(getSyncStatusIcon(SYNC_DISABLED));
        }
        setTitle(mAccount.name);
        setSummary("");
        setPersistent(false);
        setSyncStatus(SYNC_DISABLED, false);
    }

    public Account getAccount() {
        return mAccount;
    }

    public ArrayList<String> getAuthorities() {
        return mAuthorities;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (!mShowTypeIcon) {
            mSyncStatusIcon = (ImageView) view.findViewById(android.R.id.icon);
            mSyncStatusIcon.setImageResource(getSyncStatusIcon(mStatus));
            mSyncStatusIcon.setContentDescription(getSyncContentDescription(mStatus));
        }
    }

    public void setSyncStatus(int status, boolean updateSummary) {
        if (mStatus == status) {
            Log.d(TAG, "Status is the same, not changing anything");
            return;
        }
        mStatus = status;
        if (!mShowTypeIcon && mSyncStatusIcon != null) {
            mSyncStatusIcon.setImageResource(getSyncStatusIcon(status));
            mSyncStatusIcon.setContentDescription(getSyncContentDescription(mStatus));
        }
        if (updateSummary) {
            setSummary(getSyncStatusMessage(status));
        }
    }

    private int getSyncStatusMessage(int status) {
        int res;
        switch (status) {
            case SYNC_ENABLED:
                res = R.string.sync_enabled;
                break;
            case SYNC_DISABLED:
                res = R.string.sync_disabled;
                break;
            case SYNC_ERROR:
                res = R.string.sync_error;
                break;
            case SYNC_IN_PROGRESS:
                res = R.string.sync_in_progress;
                break;
            default:
                res = R.string.sync_error;
                Log.e(TAG, "Unknown sync status: " + status);
        }
        return res;
    }

    private int getSyncStatusIcon(int status) {
        int res;
        switch (status) {
            case SYNC_ENABLED:
            case SYNC_IN_PROGRESS:
                res = R.drawable.ic_settings_sync;
                break;
            case SYNC_DISABLED:
                res = R.drawable.ic_settings_sync_disabled;
                break;
            case SYNC_ERROR:
                res = R.drawable.ic_settings_sync_failed;
                break;
            default:
                res = R.drawable.ic_settings_sync_failed;
                Log.e(TAG, "Unknown sync status: " + status);
        }
        return res;
    }

    private String getSyncContentDescription(int status) {
        switch (status) {
            case SYNC_ENABLED:
                return getContext().getString(R.string.accessibility_sync_enabled);
            case SYNC_DISABLED:
                return getContext().getString(R.string.accessibility_sync_disabled);
            case SYNC_ERROR:
                return getContext().getString(R.string.accessibility_sync_error);
            case SYNC_IN_PROGRESS:
                return getContext().getString(R.string.accessibility_sync_in_progress);
            default:
                Log.e(TAG, "Unknown sync status: " + status);
                return getContext().getString(R.string.accessibility_sync_error);
        }
    }
}
