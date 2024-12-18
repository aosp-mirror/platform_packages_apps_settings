/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static com.google.common.base.Preconditions.checkNotNull;

import android.annotation.NonNull;
import android.content.Context;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.notification.modes.ZenModesBackend;

/**
 * Base class for all Settings pages controlling Modes behavior.
 */
abstract class ZenModesFragmentBase extends RestrictedDashboardFragment {
    protected static final String TAG = "ZenModesSettings";
    protected static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    protected Context mContext;
    protected ZenModesBackend mBackend;
    protected ZenHelperBackend mHelperBackend;
    private ZenSettingsObserver mSettingsObserver;

    ZenModesFragmentBase() {
        super(UserManager.DISALLOW_ADJUST_VOLUME);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void setBackend(ZenModesBackend backend) {
        mBackend = backend;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mContext = context;
        mBackend = ZenModesBackend.getInstance(context);
        mHelperBackend = ZenHelperBackend.getInstance(context);
        mSettingsObserver = new ZenSettingsObserver(context, this::onUpdatedZenModeState);
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isUiRestricted()) {
            if (isUiRestrictedByOnlyAdmin()) {
                getPreferenceScreen().removeAll();
            } else {
                finish();
            }
        }

        onUpdatedZenModeState(); // Maybe, while we weren't observing.
        checkNotNull(mSettingsObserver).register();
    }

    /**
     * Called by this fragment when we know or suspect that Zen Modes data or state has changed.
     * Individual pages must implement this method to refresh whatever they're displaying.
     */
    protected abstract void onUpdatedZenModeState();

    @Override
    public void onStop() {
        checkNotNull(mSettingsObserver).unregister();
        super.onStop();
    }
}
