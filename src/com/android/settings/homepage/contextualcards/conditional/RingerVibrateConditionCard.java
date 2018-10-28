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

package com.android.settings.homepage.contextualcards.conditional;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

public class RingerVibrateConditionCard implements ConditionalCard {

    private final Context mAppContext;

    public RingerVibrateConditionCard(Context appContext) {
        mAppContext = appContext;
    }

    @Override
    public long getId() {
        return RingerVibrateConditionController.ID;
    }

    @Override
    public CharSequence getActionText() {
        return mAppContext.getText(R.string.condition_device_muted_action_turn_on_sound);
    }

    @Override
    public int getMetricsConstant() {
        return MetricsProto.MetricsEvent.SETTINGS_CONDITION_DEVICE_VIBRATE;
    }

    @Override
    public Drawable getIcon() {
        return mAppContext.getDrawable(R.drawable.ic_volume_ringer_vibrate);
    }

    @Override
    public CharSequence getTitle() {
        return mAppContext.getText(R.string.condition_device_vibrate_title);
    }

    @Override
    public CharSequence getSummary() {
        return mAppContext.getText(R.string.condition_device_vibrate_summary);
    }
}
