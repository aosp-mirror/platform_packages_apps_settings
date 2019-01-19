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

public class RingerMutedConditionController extends AbnormalRingerConditionController {
    static final int ID = Objects.hash("RingerMutedConditionController");

    private final Context mAppContext;

    public RingerMutedConditionController(Context appContext, ConditionManager conditionManager) {
        super(appContext, conditionManager);
        mAppContext = appContext;
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public boolean isDisplayable() {
        return mAudioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_SILENT;
    }

    @Override
    public ContextualCard buildContextualCard() {
        return new ConditionalContextualCard.Builder()
                .setConditionId(ID)
                .setMetricsConstant(SettingsEnums.SETTINGS_CONDITION_DEVICE_MUTED)
                .setActionText(
                        mAppContext.getText(R.string.condition_device_muted_action_turn_on_sound))
                .setName(mAppContext.getPackageName() + "/"
                        + mAppContext.getText(R.string.condition_device_muted_title))
                .setTitleText(mAppContext.getText(R.string.condition_device_muted_title).toString())
                .setSummaryText(
                        mAppContext.getText(R.string.condition_device_muted_summary).toString())
                .setIconDrawable(mAppContext.getDrawable(R.drawable.ic_notifications_off_24dp))
                .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_HALF_WIDTH)
                .build();
    }
}
