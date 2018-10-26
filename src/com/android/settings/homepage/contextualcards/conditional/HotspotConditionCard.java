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
import android.os.UserHandle;
import android.os.UserManager;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtilsInternal;

public class HotspotConditionCard implements ConditionalCard {

    private final Context mAppContext;
    private final ConditionManager mConditionManager;

    public HotspotConditionCard(Context appContext, ConditionManager manager) {
        mAppContext = appContext;
        mConditionManager = manager;
    }

    @Override
    public long getId() {
        return HotspotConditionController.ID;
    }

    @Override
    public CharSequence getActionText() {
        if (RestrictedLockUtilsInternal.hasBaseUserRestriction(mAppContext,
                UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.myUserId())) {
            return null;
        }
        return mAppContext.getText(R.string.condition_turn_off);
    }

    @Override
    public int getMetricsConstant() {
        return MetricsProto.MetricsEvent.SETTINGS_CONDITION_HOTSPOT;
    }

    @Override
    public Drawable getIcon() {
        return mAppContext.getDrawable(R.drawable.ic_hotspot);
    }

    @Override
    public CharSequence getTitle() {
        return mAppContext.getText(R.string.condition_hotspot_title);
    }

    @Override
    public CharSequence getSummary() {
        final HotspotConditionController controller = mConditionManager.getController(getId());
        return controller.getSummary();
    }
}
