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

package com.android.settings.dashboard.conditional;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.PersistableBundle;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import static android.content.pm.PackageManager.DONT_KILL_APP;

public abstract class Condition {

    private static final String KEY_SILENCE = "silence";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_LAST_STATE = "last_state";

    protected final ConditionManager mManager;

    private boolean mIsSilenced;
    private boolean mIsActive;
    private long mLastStateChange;

    // All conditions must live in this package.
    Condition(ConditionManager manager) {
        mManager = manager;
        Class<?> receiverClass = getReceiverClass();
        if (receiverClass != null && shouldAlwaysListenToBroadcast()) {
            PackageManager pm = mManager.getContext().getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(mManager.getContext(), receiverClass),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP);
        }
    }

    void restoreState(PersistableBundle bundle) {
        mIsSilenced = bundle.getBoolean(KEY_SILENCE);
        mIsActive = bundle.getBoolean(KEY_ACTIVE);
        mLastStateChange = bundle.getLong(KEY_LAST_STATE);
    }

    boolean saveState(PersistableBundle bundle) {
        if (mIsSilenced) {
            bundle.putBoolean(KEY_SILENCE, mIsSilenced);
        }
        if (mIsActive) {
            bundle.putBoolean(KEY_ACTIVE, mIsActive);
            bundle.putLong(KEY_LAST_STATE, mLastStateChange);
        }
        return mIsSilenced || mIsActive;
    }

    protected void notifyChanged() {
        mManager.notifyChanged(this);
    }

    public boolean isSilenced() {
        return mIsSilenced;
    }

    public boolean isActive() {
        return mIsActive;
    }

    protected void setActive(boolean active) {
        if (mIsActive == active) {
            return;
        }
        mIsActive = active;
        mLastStateChange = System.currentTimeMillis();
        if (mIsSilenced && !active) {
            mIsSilenced = false;
            onSilenceChanged(mIsSilenced);
        }
        notifyChanged();
    }

    public void silence() {
        if (!mIsSilenced) {
            mIsSilenced = true;
            MetricsLogger.action(mManager.getContext(),
                    MetricsEvent.ACTION_SETTINGS_CONDITION_DISMISS, getMetricsConstant());
            onSilenceChanged(mIsSilenced);
            notifyChanged();
        }
    }

    private void onSilenceChanged(boolean silenced) {
        if (shouldAlwaysListenToBroadcast()) {
            // Don't try to disable BroadcastReceiver if we want it always on.
            return;
        }
        Class<?> clz = getReceiverClass();
        if (clz == null) {
            return;
        }
        // Only need to listen for changes when its been silenced.
        PackageManager pm = mManager.getContext().getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(mManager.getContext(), clz),
                silenced ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                DONT_KILL_APP);
    }

    protected Class<?> getReceiverClass() {
        return null;
    }

    protected boolean shouldAlwaysListenToBroadcast() {
        return false;
    }

    public boolean shouldShow() {
        return isActive() && !isSilenced();
    }

    long getLastChange() {
        return mLastStateChange;
    }

    // State.
    public abstract void refreshState();

    public abstract int getMetricsConstant();

    // UI.
    public abstract Icon getIcon();
    public abstract CharSequence getTitle();
    public abstract CharSequence getSummary();
    public abstract CharSequence[] getActions();

    public abstract void onPrimaryClick();
    public abstract void onActionClick(int index);
}
