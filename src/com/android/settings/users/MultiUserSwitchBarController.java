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

package com.android.settings.users;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class MultiUserSwitchBarController implements SwitchWidgetController.OnSwitchChangeListener,
        LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "MultiUserSwitchBarCtrl";

    interface OnMultiUserSwitchChangedListener {
        void onMultiUserSwitchChanged(boolean newState);
    }
    @VisibleForTesting
    final SwitchWidgetController mSwitchBar;

    private final Context mContext;
    private final UserCapabilities mUserCapabilities;
    private final OnMultiUserSwitchChangedListener mListener;


    MultiUserSwitchBarController(Context context, SwitchWidgetController switchBar,
            OnMultiUserSwitchChangedListener listener) {
        mContext = context;
        mSwitchBar = switchBar;
        mListener = listener;
        mUserCapabilities = UserCapabilities.create(context);
        mSwitchBar.setChecked(mUserCapabilities.mUserSwitcherEnabled);

        if (mUserCapabilities.mDisallowSwitchUser) {
            mSwitchBar.setDisabledByAdmin(RestrictedLockUtilsInternal
                    .checkIfRestrictionEnforced(mContext, UserManager.DISALLOW_USER_SWITCH,
                            UserHandle.myUserId()));
        } else {
            mSwitchBar.setEnabled(!mUserCapabilities.mDisallowSwitchUser
                    && !mUserCapabilities.mIsGuest && mUserCapabilities.isAdmin());
        }
        mSwitchBar.setListener(this);
    }

    @Override
    public void onStart() {
        mSwitchBar.startListening();
    }

    @Override
    public void onStop() {
        mSwitchBar.stopListening();
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        Log.d(TAG, "Toggling multi-user feature enabled state to: " + isChecked);
        final boolean success = Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.USER_SWITCHER_ENABLED, isChecked ? 1 : 0);
        if (success && mListener != null) {
            mListener.onMultiUserSwitchChanged(isChecked);
        }
        return success;
    }
}
