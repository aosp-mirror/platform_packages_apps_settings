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

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;

public class DndCondition extends Condition {

    private static final String TAG = "DndCondition";
    private static final String KEY_STATE = "state";

    private int mZen;
    private ZenModeConfig mConfig;

    public DndCondition(ConditionManager manager) {
        super(manager);
    }

    @Override
    public void refreshState() {
        NotificationManager notificationManager =
                mManager.getContext().getSystemService(NotificationManager.class);
        mZen = notificationManager.getZenMode();
        boolean zenModeEnabled = mZen != Settings.Global.ZEN_MODE_OFF;
        if (zenModeEnabled) {
            mConfig = notificationManager.getZenModeConfig();
        } else {
            mConfig = null;
        }
        setActive(zenModeEnabled);
    }

    @Override
    boolean saveState(PersistableBundle bundle) {
        bundle.putInt(KEY_STATE, mZen);
        return super.saveState(bundle);
    }

    @Override
    void restoreState(PersistableBundle bundle) {
        super.restoreState(bundle);
        mZen = bundle.getInt(KEY_STATE, Global.ZEN_MODE_OFF);
    }

    @Override
    protected Class<?> getReceiverClass() {
        return Receiver.class;
    }

    private CharSequence getZenState() {
        switch (mZen) {
            case Settings.Global.ZEN_MODE_ALARMS:
                return mManager.getContext().getString(R.string.zen_mode_option_alarms);
            case Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                return mManager.getContext().getString(
                        R.string.zen_mode_option_important_interruptions);
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                return mManager.getContext().getString(R.string.zen_mode_option_no_interruptions);
        }
        return null;
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(mManager.getContext(), R.drawable.ic_zen);
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getString(R.string.condition_zen_title, getZenState());
    }

    @Override
    public CharSequence getSummary() {
        final boolean isForever = mConfig != null && mConfig.manualRule != null
                && mConfig.manualRule.conditionId == null;
        return isForever ? mManager.getContext().getString(com.android.internal.R.string.zen_mode_forever_dnd)
                : ZenModeConfig.getConditionSummary(mManager.getContext(), mConfig,
                ActivityManager.getCurrentUser(),
                false);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] { mManager.getContext().getString(R.string.condition_turn_off) };
    }

    @Override
    public void onPrimaryClick() {
        StatusBarManager statusBar = mManager.getContext().getSystemService(StatusBarManager.class);
        statusBar.expandSettingsPanel("dnd");
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            NotificationManager notificationManager = mManager.getContext().getSystemService(
                    NotificationManager.class);
            notificationManager.setZenMode(Settings.Global.ZEN_MODE_OFF, null, TAG);
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + index);
        }
    }

    @Override
    public int getMetricsConstant() {
        return MetricsEvent.SETTINGS_CONDITION_DND;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED_INTERNAL
                    .equals(intent.getAction())) {
                ConditionManager.get(context).getCondition(DndCondition.class)
                        .refreshState();
            }
        }
    }

    @Override
    protected boolean shouldAlwaysListenToBroadcast() {
        return true;
    }
}
