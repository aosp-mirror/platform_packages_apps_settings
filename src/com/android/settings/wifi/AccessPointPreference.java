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
 * limitations under the License.
 */
package com.android.settings.wifi;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.wifi.WifiConfiguration;
import android.os.Looper;
import android.os.UserHandle;
import android.preference.Preference;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

public class AccessPointPreference extends Preference {

    private static final int[] STATE_SECURED = {
        R.attr.state_encrypted
    };
    private static final int[] STATE_NONE = {};

    private static int[] wifi_signal_attributes = { R.attr.wifi_signal };

    private TextView mTitleView;
    private TextView mSummaryView;
    private boolean mShowSummary = true;
    private boolean mForSavedNetworks = false;
    private AccessPoint mAccessPoint;
    private Drawable mBadge;
    private int mBadgePadding;
    private int mLevel;

    public AccessPointPreference(AccessPoint accessPoint, Context context,
                                 boolean forSavedNetworks) {
        super(context);
        mAccessPoint = accessPoint;
        mForSavedNetworks = forSavedNetworks;
        mAccessPoint.setTag(this);

        refresh();
    }

    public AccessPoint getAccessPoint() {
        return mAccessPoint;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Drawable drawable = getIcon();
        if (drawable != null) {
            drawable.setLevel(mLevel);
        }

        mTitleView = (TextView) view.findViewById(com.android.internal.R.id.title);
        if (mTitleView != null) {
            // Attach to the end of the title view
            mTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, mBadge, null);
            mTitleView.setCompoundDrawablePadding(mBadgePadding);
        }

        mSummaryView = (TextView) view.findViewById(com.android.internal.R.id.summary);
        mSummaryView.setVisibility(mShowSummary ? View.VISIBLE : View.GONE);

        updateBadge(getContext());
    }

    protected void updateIcon(int level, Context context) {
        if (level == -1) {
            setIcon(null);
        } else {
            if (getIcon() == null) {
                // To avoid a drawing race condition, we first set the state (SECURE/NONE) and then
                // set the icon (drawable) to that state's drawable.
                StateListDrawable sld = (StateListDrawable) context.getTheme()
                        .obtainStyledAttributes(wifi_signal_attributes).getDrawable(0);
                // If sld is null then we are indexing and therefore do not have access to
                // (nor need to display) the drawable.
                if (sld != null) {
                    sld.setState((mAccessPoint.getSecurity() != AccessPoint.SECURITY_NONE)
                            ? STATE_SECURED
                            : STATE_NONE);
                    Drawable drawable = sld.getCurrent();
                    if (!mForSavedNetworks) {
                        setIcon(drawable);
                    } else {
                        setIcon(null);
                    }
                }
            }
        }
    }

    protected void updateBadge(Context context) {
        WifiConfiguration config = mAccessPoint.getConfig();
        if (config != null) {
            // Fetch badge (may be null)
            UserHandle creatorUser = new UserHandle(UserHandle.getUserId(config.creatorUid));
            mBadge = context.getPackageManager().getUserBadgeForDensity(creatorUser, 0 /* dpi */);

            // Distance from the end of the title at which this AP's user badge should sit.
            mBadgePadding = context.getResources()
                    .getDimensionPixelSize(R.dimen.wifi_preference_badge_padding);
        }
    }

    /**
     * Updates the title and summary; may indirectly call notifyChanged().
     */
    public void refresh() {
        if (mForSavedNetworks) {
            setTitle(mAccessPoint.getConfigName());
        } else {
            setTitle(mAccessPoint.getSsid());
        }

        final Context context = getContext();
        int level = mAccessPoint.getLevel();
        if (level != mLevel) {
            updateIcon(mLevel, context);
            mLevel = level;
            notifyChanged();
        }
        updateBadge(context);

        // Force new summary
        setSummary(null);

        String summary = mForSavedNetworks ? mAccessPoint.getSavedNetworkSummary()
                : mAccessPoint.getSettingsSummary();

        boolean showSummary = summary.length() > 0;
        if (showSummary) {
            setSummary(summary);
        }
        if (showSummary != mShowSummary) {
            mShowSummary = showSummary;
            notifyChanged();
        }
    }

    @Override
    protected void notifyChanged() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            // Let our BG thread callbacks call setTitle/setSummary.
            postNotifyChanged();
        } else {
            super.notifyChanged();
        }
    }

    public void onLevelChanged() {
        postNotifyChanged();
    }

    private void postNotifyChanged() {
        if (mTitleView != null) {
            mTitleView.post(mNotifyChanged);
        } // Otherwise we haven't been bound yet, and don't need to update.
    }

    private final Runnable mNotifyChanged = new Runnable() {
        @Override
        public void run() {
            notifyChanged();
        }
    };
}
