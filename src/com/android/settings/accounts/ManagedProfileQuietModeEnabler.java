/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.app.admin.flags.Flags;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.Utils;

import javax.annotation.Nullable;

/**
 * A class that controls the managed profile's quiet mode, listens to quiet mode changes and
 * modifies managed profile settings. The user facing term for quiet mode is "work apps".
 */
final class ManagedProfileQuietModeEnabler implements DefaultLifecycleObserver {

    private static final String TAG = "QuietModeEnabler";
    private final Context mContext;
    private final QuietModeChangeListener mListener;
    @Nullable private final UserHandle mManagedProfile;
    private final UserManager mUserManager;

    public interface QuietModeChangeListener {
        /** Called when quiet mode has changed. */
        void onQuietModeChanged();
    }

    ManagedProfileQuietModeEnabler(Context context, QuietModeChangeListener listener) {
        mContext = context;
        mListener = listener;
        mUserManager = context.getSystemService(UserManager.class);
        mManagedProfile = Utils.getManagedProfile(mUserManager);
    }

    public void setQuietModeEnabled(boolean enabled) {
        if (mManagedProfile == null) {
            return;
        }
        if (Flags.quietModeCredentialBugFix()) {
            if (isQuietModeEnabled() != enabled) {
                mUserManager.requestQuietModeEnabled(enabled, mManagedProfile);
            }
        } else {
            mUserManager.requestQuietModeEnabled(enabled, mManagedProfile);
        }
    }

    public boolean isQuietModeEnabled() {
        return mManagedProfile != null && mUserManager.isQuietModeEnabled(mManagedProfile);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        mContext.registerReceiver(mReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mContext.unregisterReceiver(mReceiver);
    }

    public boolean isAvailable() {
        return (mManagedProfile != null);
    }

    private void refreshQuietMode() {
        if (mListener != null) {
            mListener.onQuietModeChanged();
        }
    }

    /**
     * Receiver that listens to {@link Intent#ACTION_MANAGED_PROFILE_AVAILABLE} and
     * {@link Intent#ACTION_MANAGED_PROFILE_UNAVAILABLE}, and updates the work mode
     */
    @VisibleForTesting
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            Log.v(TAG, "Received broadcast: " + action);

            if (Intent.ACTION_MANAGED_PROFILE_AVAILABLE.equals(action)
                    || Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE.equals(action)) {
                int intentUserIdentifier = intent.getIntExtra(Intent.EXTRA_USER_HANDLE,
                        UserHandle.USER_NULL);
                if (intentUserIdentifier == mManagedProfile.getIdentifier()) {
                    refreshQuietMode();
                } else {
                    Log.w(TAG, "Managed profile broadcast ID: " + intentUserIdentifier
                            + " does not match managed user: " + mManagedProfile);
                }
            } else {
                Log.w(TAG, "Cannot handle received broadcast: " + intent.getAction());
            }
        }
    };
}
