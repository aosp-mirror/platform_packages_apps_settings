/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.android.internal.app.ColorDisplayController;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.display.NightDisplaySettings;

public final class NightDisplayCondition extends Condition
        implements ColorDisplayController.Callback {

    private ColorDisplayController mController;

    NightDisplayCondition(ConditionManager manager) {
        super(manager);
        mController = new ColorDisplayController(manager.getContext());
        mController.setListener(this);
    }

    @Override
    public int getMetricsConstant() {
        return MetricsEvent.SETTINGS_CONDITION_NIGHT_DISPLAY;
    }

    @Override
    public Drawable getIcon() {
        return mManager.getContext().getDrawable(R.drawable.ic_settings_night_display);
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getString(R.string.condition_night_display_title);
    }

    @Override
    public CharSequence getSummary() {
        return mManager.getContext().getString(R.string.condition_night_display_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] {mManager.getContext().getString(R.string.condition_turn_off)};
    }

    @Override
    public void onPrimaryClick() {
        new SubSettingLauncher(mManager.getContext())
                .setDestination(NightDisplaySettings.class.getName())
                .setSourceMetricsCategory(MetricsEvent.DASHBOARD_SUMMARY)
                .setTitle(R.string.night_display_title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .launch();
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            mController.setActivated(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + index);
        }
    }

    @Override
    public void refreshState() {
        setActive(mController.isActivated());
    }

    @Override
    public void onActivated(boolean activated) {
        refreshState();
    }
}
