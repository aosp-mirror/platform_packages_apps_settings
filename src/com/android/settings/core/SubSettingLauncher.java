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

package com.android.settings.core;

import android.annotation.StringRes;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class SubSettingLauncher {

    private final Context mContext;
    private final LaunchRequest mLaunchRequest;
    private boolean mLaunched;

    public SubSettingLauncher(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must be non-null.");
        }
        mContext = context;
        mLaunchRequest = new LaunchRequest();
    }

    public SubSettingLauncher setDestination(String fragmentName) {
        mLaunchRequest.destinationName = fragmentName;
        return this;
    }

    /**
     * Set title with resource string id.
     *
     * @param titleResId res id of string
     */
    public SubSettingLauncher setTitleRes(@StringRes int titleResId) {
        return setTitleRes(null /*titlePackageName*/, titleResId);
    }

    /**
     * Set title with resource string id, and package name to resolve the resource id.
     *
     * @param titlePackageName package name to resolve resource
     * @param titleResId       res id of string, will use package name to resolve
     */
    public SubSettingLauncher setTitleRes(String titlePackageName, @StringRes int titleResId) {
        mLaunchRequest.titleResPackageName = titlePackageName;
        mLaunchRequest.titleResId = titleResId;
        mLaunchRequest.title = null;
        return this;
    }

    /**
     * Set title with text,
     * This method is only for user generated string,
     * display text will not update after locale change,
     * if title string is from resource id, please use setTitleRes.
     *
     * @param title text title
     */
    public SubSettingLauncher setTitleText(CharSequence title) {
        mLaunchRequest.title = title;
        return this;
    }

    public SubSettingLauncher setArguments(Bundle arguments) {
        mLaunchRequest.arguments = arguments;
        return this;
    }

    public SubSettingLauncher setExtras(Bundle extras) {
        mLaunchRequest.extras = extras;
        return this;
    }

    public SubSettingLauncher setSourceMetricsCategory(int sourceMetricsCategory) {
        mLaunchRequest.sourceMetricsCategory = sourceMetricsCategory;
        return this;
    }

    public SubSettingLauncher setResultListener(Fragment listener, int resultRequestCode) {
        mLaunchRequest.mRequestCode = resultRequestCode;
        mLaunchRequest.mResultListener = listener;
        return this;
    }

    public SubSettingLauncher addFlags(int flags) {
        mLaunchRequest.flags |= flags;
        return this;
    }

    public SubSettingLauncher setUserHandle(UserHandle userHandle) {
        mLaunchRequest.userHandle = userHandle;
        return this;
    }

    public void launch() {
        if (mLaunched) {
            throw new IllegalStateException(
                    "This launcher has already been executed. Do not reuse");
        }
        mLaunched = true;

        final Intent intent = toIntent();

        boolean launchAsUser = mLaunchRequest.userHandle != null
                && mLaunchRequest.userHandle.getIdentifier() != UserHandle.myUserId();
        boolean launchForResult = mLaunchRequest.mResultListener != null;
        if (launchAsUser && launchForResult) {
            launchForResultAsUser(intent, mLaunchRequest.userHandle, mLaunchRequest.mResultListener,
                    mLaunchRequest.mRequestCode);
        } else if (launchAsUser && !launchForResult) {
            launchAsUser(intent, mLaunchRequest.userHandle);
        } else if (!launchAsUser && launchForResult) {
            launchForResult(mLaunchRequest.mResultListener, intent, mLaunchRequest.mRequestCode);
        } else {
            launch(intent);
        }
    }

    public Intent toIntent() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        copyExtras(intent);
        intent.setClass(mContext, SubSettings.class);
        if (TextUtils.isEmpty(mLaunchRequest.destinationName)) {
            throw new IllegalArgumentException("Destination fragment must be set");
        }
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, mLaunchRequest.destinationName);

        if (mLaunchRequest.sourceMetricsCategory < 0) {
            throw new IllegalArgumentException("Source metrics category must be set");
        }
        intent.putExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                mLaunchRequest.sourceMetricsCategory);

        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, mLaunchRequest.arguments);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME,
                mLaunchRequest.titleResPackageName);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID,
                mLaunchRequest.titleResId);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE, mLaunchRequest.title);
        intent.addFlags(mLaunchRequest.flags);
        return intent;
    }

    @VisibleForTesting
    void launch(Intent intent) {
        mContext.startActivity(intent);
    }

    @VisibleForTesting
    void launchAsUser(Intent intent, UserHandle userHandle) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivityAsUser(intent, userHandle);
    }

    @VisibleForTesting
    void launchForResultAsUser(Intent intent, UserHandle userHandle,
            Fragment resultListener, int requestCode) {
        resultListener.getActivity().startActivityForResultAsUser(intent, requestCode, userHandle);
    }

    private void launchForResult(Fragment listener, Intent intent, int requestCode) {
        listener.startActivityForResult(intent, requestCode);
    }

    private void copyExtras(Intent intent) {
        if (mLaunchRequest.extras != null) {
            intent.replaceExtras(mLaunchRequest.extras);
        }
    }
    /**
     * Simple container that has information about how to launch a subsetting.
     */
    static class LaunchRequest {
        String destinationName;
        int titleResId;
        String titleResPackageName;
        CharSequence title;
        int sourceMetricsCategory = -100;
        int flags;
        Fragment mResultListener;
        int mRequestCode;
        UserHandle userHandle;
        Bundle arguments;
        Bundle extras;
    }
}
