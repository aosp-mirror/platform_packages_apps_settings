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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import androidx.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.ZenModeSettings;

public class DndCondition extends Condition {

    private static final String TAG = "DndCondition";
    private static final String KEY_STATE = "state";

    private boolean mRegistered;

    @VisibleForTesting
    static final IntentFilter DND_FILTER =
        new IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED_INTERNAL);
    @VisibleForTesting
    protected ZenModeConfig mConfig;

    private int mZen;
    private final Receiver mReceiver;

    public DndCondition(ConditionManager manager) {
        super(manager);
        mReceiver = new Receiver();
        mManager.getContext().registerReceiver(mReceiver, DND_FILTER);
        mRegistered = true;
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
    public Drawable getIcon() {
        return mManager.getContext().getDrawable(R.drawable.ic_do_not_disturb_on_24dp);
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getString(R.string.condition_zen_title);
    }

    @Override
    public CharSequence getSummary() {
        return ZenModeConfig.getDescription(mManager.getContext(), mZen != Global.ZEN_MODE_OFF,
                mConfig, true);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] { mManager.getContext().getString(R.string.condition_turn_off) };
    }

    @Override
    public void onPrimaryClick() {
        new SubSettingLauncher(mManager.getContext())
                .setDestination(ZenModeSettings.class.getName())
                .setSourceMetricsCategory(MetricsEvent.DASHBOARD_SUMMARY)
                .setTitle(R.string.zen_mode_settings_title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .launch();
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
                final Condition condition =
                        ConditionManager.get(context).getCondition(DndCondition.class);
                if (condition != null) {
                    condition.refreshState();
                }
            }
        }
    }

    @Override
    public void onResume() {
        if (!mRegistered) {
           mManager.getContext().registerReceiver(mReceiver, DND_FILTER);
           mRegistered = true;
        }
    }

    @Override
    public void onPause() {
        if (mRegistered) {
            mManager.getContext().unregisterReceiver(mReceiver);
            mRegistered = false;
        }
    }
}
