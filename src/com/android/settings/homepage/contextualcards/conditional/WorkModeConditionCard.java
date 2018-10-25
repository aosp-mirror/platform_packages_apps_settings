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

public class WorkModeConditionCard implements ConditionalCard {

    private final Context mAppContext;

    public WorkModeConditionCard(Context appContext) {
        mAppContext = appContext;
    }

    @Override
    public long getId() {
        return WorkModeConditionController.ID;
    }

    @Override
    public CharSequence getActionText() {
        return mAppContext.getText(R.string.condition_turn_on);
    }

    @Override
    public int getMetricsConstant() {
        return MetricsProto.MetricsEvent.SETTINGS_CONDITION_WORK_MODE;
    }

    @Override
    public Drawable getIcon() {
        return mAppContext.getDrawable(R.drawable.ic_signal_workmode_enable);
    }

    @Override
    public CharSequence getTitle() {
        return mAppContext.getText(R.string.condition_work_title);
    }

    @Override
    public CharSequence getSummary() {
        return mAppContext.getText(R.string.condition_work_summary);
    }

}
