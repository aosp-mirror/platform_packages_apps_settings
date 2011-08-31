/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import java.util.ArrayList;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * AccountPreference is used to display a username, status and provider icon for an account on
 * the device.
 */
public class AccountPreference extends Preference {
    private static final String TAG = "AccountPreference";
    public static final int SYNC_ENABLED = 0; // all know sync adapters are enabled and OK
    public static final int SYNC_DISABLED = 1; // no sync adapters are enabled
    public static final int SYNC_ERROR = 2; // one or more sync adapters have a problem
    private int mStatus;
    private Account mAccount;
    private ArrayList<String> mAuthorities;
    private Drawable mProviderIcon;
    private ImageView mSyncStatusIcon;
    private ImageView mProviderIconView;

    public AccountPreference(Context context, Account account, Drawable icon,
            ArrayList<String> authorities) {
        super(context);
        mAccount = account;
        mAuthorities = authorities;
        mProviderIcon = icon;
        setWidgetLayoutResource(R.layout.account_preference);
        setTitle(mAccount.name);
        setSummary("");
        setPersistent(false);
        setSyncStatus(SYNC_DISABLED);
        setIcon(mProviderIcon);
    }

    public Account getAccount() {
        return mAccount;
    }

    public ArrayList<String> getAuthorities() {
        return mAuthorities;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        setSummary(getSyncStatusMessage(mStatus));
        mSyncStatusIcon = (ImageView) view.findViewById(R.id.syncStatusIcon);
        mSyncStatusIcon.setImageResource(getSyncStatusIcon(mStatus));
        mSyncStatusIcon.setContentDescription(getSyncContentDescription(mStatus));
    }

    public void setProviderIcon(Drawable icon) {
        mProviderIcon = icon;
        if (mProviderIconView != null) {
            mProviderIconView.setImageDrawable(icon);
        }
    }

    public void setSyncStatus(int status) {
        mStatus = status;
        if (mSyncStatusIcon != null) {
            mSyncStatusIcon.setImageResource(getSyncStatusIcon(status));
        }
        setSummary(getSyncStatusMessage(status));
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
                res = R.drawable.ic_sync_green_holo;
                break;
            case SYNC_DISABLED:
                res = R.drawable.ic_sync_grey_holo;
                break;
            case SYNC_ERROR:
                res = R.drawable.ic_sync_red_holo;
                break;
            default:
                res = R.drawable.ic_sync_red_holo;
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
            default:
                Log.e(TAG, "Unknown sync status: " + status);
                return getContext().getString(R.string.accessibility_sync_error);
        }
    }

    @Override
    public int compareTo(Preference other) {
        if (!(other instanceof AccountPreference)) {
            // Put other preference types above us
            return 1;
        }
        return mAccount.name.compareTo(((AccountPreference) other).mAccount.name);
    }
}
