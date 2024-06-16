/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.notification.modes;

import android.content.Context;

import com.android.settingslib.RestrictedPreference;

/**
 * Preference representing a single mode item on the modes aggregator page. Clicking on this
 * preference leads to an individual mode's configuration page.
 */
class ZenModesListItemPreference extends RestrictedPreference {
    final Context mContext;
    ZenMode mZenMode;

    ZenModesListItemPreference(Context context, ZenMode zenMode) {
        super(context);
        mContext = context;
        setZenMode(zenMode);
        setKey(zenMode.getId());
    }

    @Override
    public void onClick() {
        ZenSubSettingLauncher.forMode(mContext, mZenMode.getId()).launch();
    }

    public void setZenMode(ZenMode zenMode) {
        mZenMode = zenMode;
        setTitle(mZenMode.getRule().getName());
        setSummary(mZenMode.getRule().getTriggerDescription());
        setIconSize(ICON_SIZE_SMALL);

        FutureUtil.whenDone(
                mZenMode.getIcon(mContext, IconLoader.getInstance()),
                icon -> setIcon(IconUtil.applyTint(mContext, icon)),
                mContext.getMainExecutor());
    }
}
