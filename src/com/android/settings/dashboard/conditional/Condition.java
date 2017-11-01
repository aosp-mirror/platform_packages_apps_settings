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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.PersistableBundle;

import android.support.annotation.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

public abstract class Condition {

    private static final String KEY_SILENCE = "silence";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_LAST_STATE = "last_state";

    protected final ConditionManager mManager;
    protected final MetricsFeatureProvider mMetricsFeatureProvider;
    protected boolean mReceiverRegistered;

    private boolean mIsSilenced;
    private boolean mIsActive;
    private long mLastStateChange;

    // All conditions must live in this package.
    Condition(ConditionManager manager) {
       this(manager, FeatureFactory.getFactory(manager.getContext()).getMetricsFeatureProvider());
    }

    Condition(ConditionManager manager, MetricsFeatureProvider metricsFeatureProvider) {
        mManager = manager;
        mMetricsFeatureProvider = metricsFeatureProvider;
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
            Context context = mManager.getContext();
            mMetricsFeatureProvider.action(context, MetricsEvent.ACTION_SETTINGS_CONDITION_DISMISS,
                    getMetricsConstant());
            onSilenceChanged(mIsSilenced);
            notifyChanged();
        }
    }

    @VisibleForTesting
    void onSilenceChanged(boolean silenced) {
        final BroadcastReceiver receiver = getReceiver();
        if (receiver == null) {
            return;
        }
        if (silenced) {
            if (!mReceiverRegistered) {
                mManager.getContext().registerReceiver(receiver, getIntentFilter());
                mReceiverRegistered = true;
            }
        } else {
            if (mReceiverRegistered) {
                mManager.getContext().unregisterReceiver(receiver);
                mReceiverRegistered = false;
            }
        }
    }

    protected BroadcastReceiver getReceiver() {
        return null;
    }

    protected IntentFilter getIntentFilter() {
        return null;
    }

    public boolean shouldShow() {
        return isActive() && !isSilenced();
    }

    long getLastChange() {
        return mLastStateChange;
    }

    public void onResume() {
    }

    public void onPause() {
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
