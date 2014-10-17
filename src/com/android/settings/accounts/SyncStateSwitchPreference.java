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

package com.android.settings.accounts;

import android.accounts.Account;
import android.app.ActivityManager;
import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.widget.AnimatedImageView;

public class SyncStateSwitchPreference extends SwitchPreference {

    private boolean mIsActive = false;
    private boolean mIsPending = false;
    private boolean mFailed = false;
    private Account mAccount;
    private String mAuthority;

    /**
     * A mode for this preference where clicking does a one-time sync instead of
     * toggling whether the provider will do autosync.
     */
    private boolean mOneTimeSyncMode = false;

    public SyncStateSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_widget_sync_toggle);
        mAccount = null;
        mAuthority = null;
    }

    public SyncStateSwitchPreference(Context context, Account account, String authority) {
        super(context, null);
        mAccount = account;
        mAuthority = authority;
        setWidgetLayoutResource(R.layout.preference_widget_sync_toggle);
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        final AnimatedImageView syncActiveView = (AnimatedImageView) view.findViewById(
                R.id.sync_active);
        final View syncFailedView = view.findViewById(R.id.sync_failed);

        final boolean activeVisible = mIsActive || mIsPending;
        syncActiveView.setVisibility(activeVisible ? View.VISIBLE : View.GONE);
        syncActiveView.setAnimating(mIsActive);

        final boolean failedVisible = mFailed && !activeVisible;
        syncFailedView.setVisibility(failedVisible ? View.VISIBLE : View.GONE);

        View switchView = view.findViewById(com.android.internal.R.id.switchWidget);
        if (mOneTimeSyncMode) {
            switchView.setVisibility(View.GONE);

            /*
             * Override the summary. Fill in the %1$s with the existing summary
             * (what ends up happening is the old summary is shown on the next
             * line).
             */
            TextView summary = (TextView) view.findViewById(android.R.id.summary);
            summary.setText(getContext().getString(R.string.sync_one_time_sync, getSummary()));
        } else {
            switchView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Set whether the sync is active.
     * @param isActive whether or not the sync is active
     */
    public void setActive(boolean isActive) {
        mIsActive = isActive;
        notifyChanged();
    }

    /**
     * Set whether a sync is pending.
     * @param isPending whether or not the sync is pending
     */
    public void setPending(boolean isPending) {
        mIsPending = isPending;
        notifyChanged();
    }

    /**
     * Set whether the corresponding sync failed.
     * @param failed whether or not the sync failed
     */
    public void setFailed(boolean failed) {
        mFailed = failed;
        notifyChanged();
    }

    /**
     * Sets whether the preference is in one-time sync mode.
     */
    public void setOneTimeSyncMode(boolean oneTimeSyncMode) {
        mOneTimeSyncMode = oneTimeSyncMode;
        notifyChanged();
    }

    /**
     * Gets whether the preference is in one-time sync mode.
     */
    public boolean isOneTimeSyncMode() {
        return mOneTimeSyncMode;
    }

    @Override
    protected void onClick() {
        // When we're in one-time sync mode, we don't want a click to change the
        // Switch state
        if (!mOneTimeSyncMode) {
            if (ActivityManager.isUserAMonkey()) {
                Log.d("SyncState", "ignoring monkey's attempt to flip sync state");
            } else {
                super.onClick();
            }
        }
    }

    public Account getAccount() {
        return mAccount;
    }

    public String getAuthority() {
        return mAuthority;
    }
}
