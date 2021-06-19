/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.applications.intentpicker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainOwner;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;

import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 *  A buffer of the supported link data. This {@link SupportedLinkWrapper} wraps the host, enabled
 *  and a list of {@link DomainOwner}.
 */
public class SupportedLinkWrapper implements Comparable {
    private static final String TAG = "SupportedLinkWrapper";

    private String mHost;
    private SortedSet<DomainOwner> mOwnerSet;
    private boolean mIsEnabled;
    private String mLastOwnerName;
    private boolean mIsChecked;

    public SupportedLinkWrapper(Context context, String host, SortedSet<DomainOwner> ownerSet) {
        mHost = host;
        mOwnerSet = ownerSet;
        mIsEnabled = true;
        mLastOwnerName = "";
        mIsChecked = false;
        init(context);
    }

    private void init(Context context) {
        if (mOwnerSet.size() > 0) {
            final long nonOverirideableNo = mOwnerSet.stream()
                    .filter(it -> !it.isOverrideable())
                    .count();
            mIsEnabled = (nonOverirideableNo == 0L);
            if (nonOverirideableNo > 0L) {
                mLastOwnerName = getLastPackageLabel(context, false);
            } else {
                mLastOwnerName = getLastPackageLabel(context, true);
            }
        }
    }

    private String getLastPackageLabel(Context context, boolean isOverrideable) {
        final List<String> labelList = mOwnerSet.stream()
                .filter(it -> it.isOverrideable() == isOverrideable)
                .map(it -> getLabel(context, it.getPackageName()))
                .filter(label -> label != null)
                .collect(Collectors.toList());
        return labelList.get(labelList.size() - 1);
    }

    private String getLabel(Context context, String pkg) {
        try {
            final PackageManager pm = context.getPackageManager();
            return pm.getApplicationInfo(pkg, /* flags= */ 0).loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "getLabel error : " + e.getMessage());
            return null;
        }
    }

    /** Returns the enabled/disabled value for list item. */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /** Returns the display format of list item in the Supported Links dialog */
    public String getDisplayTitle(Context context) {
        if (TextUtils.isEmpty(mLastOwnerName) || context == null) {
            return mHost;
        }
        return mHost + System.lineSeparator() + context.getString(
                R.string.app_launch_supported_links_subtext, mLastOwnerName);
    }

    /** Returns the host name. */
    public String getHost() {
        return mHost;
    }

    /** Returns the checked value for list item. */
    public boolean isChecked() {
        return mIsChecked;
    }

    /** Set the checked value. */
    public void setChecked(boolean isChecked) {
        mIsChecked = isChecked;
    }

    @Override
    public int compareTo(Object o) {
        final SupportedLinkWrapper that = (SupportedLinkWrapper) o;
        if (this.mIsEnabled != that.mIsEnabled) {
            return this.mIsEnabled ? -1 : 1;
        }
        if (TextUtils.isEmpty(this.mLastOwnerName) != TextUtils.isEmpty(that.mLastOwnerName)) {
            return TextUtils.isEmpty(this.mLastOwnerName) ? -1 : 1;
        }
        return 0;
    }
}
