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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.transition.SettingsTransitionHelper.TransitionType;

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
        mLaunchRequest.mTransitionType = TransitionType.TRANSITION_SHARED_AXIS;
    }

    public SubSettingLauncher setDestination(String fragmentName) {
        mLaunchRequest.mDestinationName = fragmentName;
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
        mLaunchRequest.mTitleResPackageName = titlePackageName;
        mLaunchRequest.mTitleResId = titleResId;
        mLaunchRequest.mTitle = null;
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
        mLaunchRequest.mTitle = title;
        return this;
    }

    public SubSettingLauncher setArguments(Bundle arguments) {
        mLaunchRequest.mArguments = arguments;
        return this;
    }

    public SubSettingLauncher setExtras(Bundle extras) {
        mLaunchRequest.mExtras = extras;
        return this;
    }

    public SubSettingLauncher setSourceMetricsCategory(int sourceMetricsCategory) {
        mLaunchRequest.mSourceMetricsCategory = sourceMetricsCategory;
        return this;
    }

    public SubSettingLauncher setResultListener(Fragment listener, int resultRequestCode) {
        mLaunchRequest.mRequestCode = resultRequestCode;
        mLaunchRequest.mResultListener = listener;
        return this;
    }

    public SubSettingLauncher addFlags(int flags) {
        mLaunchRequest.mFlags |= flags;
        return this;
    }

    public SubSettingLauncher setUserHandle(UserHandle userHandle) {
        mLaunchRequest.mUserHandle = userHandle;
        return this;
    }

    public SubSettingLauncher setTransitionType(int transitionType) {
        mLaunchRequest.mTransitionType = transitionType;
        return this;
    }

    /** Decide whether the next page is second layer page or not. */
    public SubSettingLauncher setIsSecondLayerPage(boolean isSecondLayerPage) {
        mLaunchRequest.mIsSecondLayerPage = isSecondLayerPage;
        return this;
    }

    public void launch() {
        launchWithIntent(toIntent());
    }

    /**
     * Launch sub settings activity with an intent.
     *
     * @param intent the settings intent we want to launch
     */
    public void launchWithIntent(@NonNull Intent intent) {
        verifyIntent(intent);
        if (mLaunched) {
            throw new IllegalStateException(
                    "This launcher has already been executed. Do not reuse");
        }
        mLaunched = true;

        boolean launchAsUser = mLaunchRequest.mUserHandle != null
                && mLaunchRequest.mUserHandle.getIdentifier() != UserHandle.myUserId();
        boolean launchForResult = mLaunchRequest.mResultListener != null;
        if (launchAsUser && launchForResult) {
            launchForResultAsUser(intent, mLaunchRequest.mUserHandle,
                    mLaunchRequest.mResultListener, mLaunchRequest.mRequestCode);
        } else if (launchAsUser && !launchForResult) {
            launchAsUser(intent, mLaunchRequest.mUserHandle);
        } else if (!launchAsUser && launchForResult) {
            launchForResult(mLaunchRequest.mResultListener, intent, mLaunchRequest.mRequestCode);
        } else {
            launch(intent);
        }
    }

    /**
     * Verify intent is correctly constructed.
     *
     * @param intent the intent to verify
     */
    @VisibleForTesting
    public void verifyIntent(@NonNull Intent intent) {
        String className = SubSettings.class.getName();
        ComponentName componentName = intent.getComponent();
        String destinationName = intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT);
        int sourceMetricsCategory =
                intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, -1);

        if (componentName != null && !TextUtils.equals(className, componentName.getClassName())) {
            throw new IllegalArgumentException(String.format("Class must be: %s", className));
        } else if (TextUtils.isEmpty(destinationName)) {
            throw new IllegalArgumentException("Destination fragment must be set");
        } else if (sourceMetricsCategory < 0) {
            throw new IllegalArgumentException("Source metrics category must be set");
        }
    }

    public Intent toIntent() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        copyExtras(intent);
        intent.setClass(mContext, SubSettings.class);
        if (TextUtils.isEmpty(mLaunchRequest.mDestinationName)) {
            throw new IllegalArgumentException("Destination fragment must be set");
        }
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, mLaunchRequest.mDestinationName);

        if (mLaunchRequest.mSourceMetricsCategory < 0) {
            throw new IllegalArgumentException("Source metrics category must be set");
        }
        intent.putExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                mLaunchRequest.mSourceMetricsCategory);

        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, mLaunchRequest.mArguments);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME,
                mLaunchRequest.mTitleResPackageName);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID,
                mLaunchRequest.mTitleResId);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE, mLaunchRequest.mTitle);
        intent.addFlags(mLaunchRequest.mFlags);
        intent.putExtra(SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
                mLaunchRequest.mTransitionType);
        intent.putExtra(SettingsActivity.EXTRA_IS_SECOND_LAYER_PAGE,
                mLaunchRequest.mIsSecondLayerPage);

        return intent;
    }

    @VisibleForTesting
    void launch(Intent intent) {
        mContext.startActivity(intent);
    }

    @VisibleForTesting
    void launchAsUser(Intent intent, UserHandle userHandle) {
        mContext.startActivityAsUser(intent, userHandle);
    }

    @VisibleForTesting
    void launchForResultAsUser(Intent intent, UserHandle userHandle,
            Fragment resultListener, int requestCode) {
        resultListener.getActivity().startActivityForResultAsUser(intent, requestCode, userHandle);
    }

    @VisibleForTesting
    void launchForResult(Fragment listener, Intent intent, int requestCode) {
        listener.startActivityForResult(intent, requestCode);
    }

    private void copyExtras(Intent intent) {
        if (mLaunchRequest.mExtras != null) {
            intent.replaceExtras(mLaunchRequest.mExtras);
        }
    }

    /**
     * Simple container that has information about how to launch a subsetting.
     */
    static class LaunchRequest {
        String mDestinationName;
        int mTitleResId;
        String mTitleResPackageName;
        CharSequence mTitle;
        int mSourceMetricsCategory = -100;
        int mFlags;
        Fragment mResultListener;
        int mRequestCode;
        UserHandle mUserHandle;
        int mTransitionType;
        Bundle mArguments;
        Bundle mExtras;
        boolean mIsSecondLayerPage;
    }
}
