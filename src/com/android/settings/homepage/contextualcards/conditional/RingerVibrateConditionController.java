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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.media.AudioManager;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;

import java.util.Objects;

public class RingerVibrateConditionController extends AbnormalRingerConditionController {
    static final int ID = Objects.hash("RingerVibrateConditionController");

    private final Context mAppContext;

    public RingerVibrateConditionController(Context appContext, ConditionManager conditionManager) {
        super(appContext, conditionManager);
        mAppContext = appContext;

    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public boolean isDisplayable() {
        return mAudioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_VIBRATE;
    }

    @Override
    public ContextualCard buildContextualCard() {
        return new ConditionalContextualCard.Builder()
                .setConditionId(ID)
                .setMetricsConstant(SettingsEnums.SETTINGS_CONDITION_DEVICE_VIBRATE)
                .setActionText(
                        mAppContext.getText(R.string.condition_device_muted_action_turn_on_sound))
                .setName(mAppContext.getPackageName() + "/"
                        + mAppContext.getText(R.string.condition_device_vibrate_title))
                .setTitleText(
                        mAppContext.getText(R.string.condition_device_vibrate_title).toString())
                .setSummaryText(
                        mAppContext.getText(R.string.condition_device_vibrate_summary).toString())
                .setIconDrawable(mAppContext.getDrawable(R.drawable.ic_volume_ringer_vibrate))
                .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_HALF_WIDTH)
                .build();
    }
}
